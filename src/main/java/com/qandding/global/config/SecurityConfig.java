package com.qandding.global.config;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.util.StringUtils;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.auth.JwtAuthenticationFilter;
import com.qandding.global.auth.JwtTokenProvider;
import com.qandding.global.auth.TokenService;
import com.qandding.global.ratelimit.RateLimitFilter;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${app.oauth.allowed-domain}")
  private String allowedDomain;

  @Value("${app.oauth.redirect-url}")
  private String redirectUrl;

  @Value("${app.jwt.cookie-domain:}")
  private String cookieDomain;

  @Value("${app.jwt.cookie-secure:false}")
  private boolean cookieSecure;

  private final UserRepository userRepository;
  private final CorsConfigurationSource corsConfigurationSource;
  private final RateLimitFilter rateLimitFilter;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final TokenService tokenService;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(session -> session
            .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
            .maximumSessions(1)
            .maxSessionsPreventsLogin(false)
        )
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
            .frameOptions(frame -> frame.sameOrigin())
            .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/api/health", "/login", "/error", "/swagger-ui/**",
                "/v3/api-docs/**", "/favicon.ico", "/api/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(oidcUserService())
                .userService(oAuth2UserService())
            )
            .successHandler(oauth2SuccessHandler())
        )
        .logout(Customizer.withDefaults())
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private OidcUserService oidcUserService() {
    return new OidcUserService() {
      @Override
      public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getEmail();
        validateDomain(email);
        return oidcUser;
      }
    };
  }

  private DefaultOAuth2UserService oAuth2UserService() {
    return new DefaultOAuth2UserService() {
      @Override
      public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String email = (String) user.getAttributes().getOrDefault("email", "");
        validateDomain(email);
        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            (Map<String, Object>) user.getAttributes(),
            "email"
        );
      }
    };
  }

  private void validateDomain(String email) {
    if (email == null || !email.endsWith("@" + allowedDomain)) {
      throw new RuntimeException("허용되지 않은 이메일 도메인입니다.");
    }
  }

  @Bean
  AuthenticationSuccessHandler oauth2SuccessHandler() {
    return (request, response, authentication) -> {
      System.out.println("=== OAuth2 로그인 성공 핸들러 시작 ===");

      Object principal = authentication.getPrincipal();
      String email;
      String name;
      if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
        email = oidc.getEmail();
        name = oidc.getFullName();
        System.out.println("OIDC User 로그인 시도");
      } else if (principal instanceof OAuth2User oAuth2) {
        email = (String) oAuth2.getAttributes().getOrDefault("email", "");
        name = (String) oAuth2.getAttributes().getOrDefault("name", "");
        System.out.println("OAuth2 User 로그인 시도");
      } else {
        throw new IllegalStateException("지원하지 않는 principal");
      }

      // 사용자 조회 또는 생성
      User user = userRepository.findByEmail(email)
          .orElseGet(() -> {
            // Google에서 받은 이름 정보 활용
            String displayName = name;
            if (displayName != null && displayName.contains("/")) {
              displayName = displayName.split("/")[0];
            }
            if (displayName == null || displayName.isBlank()) {
              displayName = "사용자";
            }

            User newUser = new User(displayName, "", "", email);
            System.out.println("새 사용자 생성됨");
            return userRepository.save(newUser);
          });

      System.out.println("사용자 정보 조회/생성 완료");

      if (!user.isEmailVerified()) {
        user.markEmailVerified();
      }

      boolean needsProfile =
          (user.getGrade() == null || user.getGrade().isBlank()) || (user.getMajor() == null
              || user.getMajor().isBlank());

      // CustomUserPrincipal 생성
      CustomUserPrincipal customUserPrincipal = new CustomUserPrincipal(
          user.getId(),
          user.getEmail(),
          user.getNickname(),
          List.of(new SimpleGrantedAuthority("ROLE_USER"))
      );

      System.out.println("CustomUserPrincipal 생성 완료");

      // 새로운 Authentication 객체 생성하여 CustomUserPrincipal 사용
      org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuthentication =
          new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
              customUserPrincipal,
              null, // credentials
              customUserPrincipal.getAuthorities()
          );

      System.out.println("새로운 Authentication 생성 완료");

      // Spring Security 컨텍스트에 새로운 인증 정보 설정
      SecurityContextHolder.getContext().setAuthentication(newAuthentication);

      // 세션에도 저장
      request.getSession().setAttribute(
          "SPRING_SECURITY_CONTEXT",
          SecurityContextHolder.getContext()
      );

      System.out.println("SecurityContext 설정 완료");

      // JWT 토큰 쌍 생성 (Access Token + Refresh Token)
      TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user);

      // Access Token을 httpOnly 쿠키로 설정
      Cookie accessCookie = new Cookie("access_token", tokenPair.getAccessToken());
      accessCookie.setHttpOnly(true);
      accessCookie.setSecure(cookieSecure); // 환경에 따라 설정
      accessCookie.setPath("/");
      accessCookie.setMaxAge(900); // 15분 (초 단위)

      // 도메인 설정 (개발/프로덕션 환경에 따라)
      if (StringUtils.hasText(cookieDomain)) {
        accessCookie.setDomain(cookieDomain);
      }

      response.addCookie(accessCookie);

      // Refresh Token은 URL 파라미터로 전달 (localStorage 저장용)
      String frontendUrl = redirectUrl +
          "?success=true&needsProfile=" + needsProfile +
          "&refreshToken=" + tokenPair.getRefreshToken();

      // 자동 리다이렉트
      response.sendRedirect(frontendUrl);

      System.out.println("=== OAuth2 로그인 성공 핸들러 완료 ===");
    };
  }
}