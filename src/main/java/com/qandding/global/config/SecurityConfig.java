package com.qandding.global.config;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.oauth.allowed-domain}")
    private String allowedDomain;

    @Value("${app.oauth.redirect-url}")
    private String redirectUrl;

    private final UserRepository userRepository;
    private final CorsConfigurationSource corsConfigurationSource;
    private final RateLimitFilter rateLimitFilter;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TokenService tokenService;

    /**
     * API 경로(/api/**)를 위한 SecurityFilterChain 입니다.
     * JWT 인증을 사용하며, 세션을 사용하지 않는 STATELESS 정책을 적용합니다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**") // 이 필터 체인은 /api/** 경로에만 적용됩니다.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/api/auth/**").permitAll() // /api/** 중 일부 경로는 인증 없이 허용
                .anyRequest().authenticated() // 그 외 모든 /api/** 요청은 인증이 필요함
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // **API 요청은 세션을 생성하거나 사용하지 않음**
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable) // API 서버는 CSRF 보호가 필요 없음 (토큰 방식 사용)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .oauth2Login(AbstractHttpConfigurer::disable) // API 서버에서는 OAuth2 로그인을 사용하지 않음
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class) // 레이트 리미트 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 인증 필터 추가

        return http.build();
    }

    /**
     * 소셜 로그인 및 그 외 웹 경로를 위한 SecurityFilterChain 입니다.
     * 세션을 사용하여 OAuth2 로그인을 처리합니다.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            // API 경로를 제외한 모든 요청을 처리
            .authorizeHttpRequests(auth -> auth
                // 소셜 로그인, 홈페이지, Swagger 등 웹 접근 경로는 모두 허용
                .requestMatchers("/", "/login/**", "/oauth2/**", "/error", "/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated() // 그 외 인증이 필요한 웹 페이지가 있다면 여기서 처리
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
                    .userService(oAuth2UserService())
                )
                .successHandler(oauth2SuccessHandler()) // OAuth2 로그인 성공 시 JWT를 발급하는 핸들러
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // OAuth2 과정에서 필요 시에만 세션 생성
            )
            .csrf(AbstractHttpConfigurer::disable) // 필요에 따라 CSRF 설정
            .logout(Customizer.withDefaults());

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

            // JWT 토큰 쌍 생성 (Access Token + Refresh Token)
            TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user);

            // 프론트엔드로 토큰 정보 전달 (URL 파라미터)
            String frontendUrl = redirectUrl +
                "?success=true&needsProfile=" + needsProfile +
                "&accessToken=" + tokenPair.getAccessToken() +
                "&refreshToken=" + tokenPair.getRefreshToken();

            // 자동 리다이렉트
            response.sendRedirect(frontendUrl);

            System.out.println("=== OAuth2 로그인 성공 핸들러 완료 ===");
        };
    }
}