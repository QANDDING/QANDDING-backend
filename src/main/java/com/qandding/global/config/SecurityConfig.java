package com.qandding.global.config;

import com.qandding.global.auth.CustomOAuth2UserService;
import com.qandding.global.auth.JwtAuthenticationFilter;
import com.qandding.global.auth.OAuth2AuthenticationFailureHandler;
import com.qandding.global.auth.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CorsConfigurationSource corsConfigurationSource;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    @Order(0)
    SecurityFilterChain swaggerChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
                        "/swagger-resources/**", "/webjars/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

  // API 엔드포인트를 위한 보안 설정 (Stateless, JWT 사용)
  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**") // /api/** 경로에만 이 필터 체인을 적용
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll() // 토큰 발급/재발급 경로는 허용
            .anyRequest().authenticated() // 나머지 API는 인증 필요
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 사용하지 않음
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 추가

    return http.build();
  }

  // 웹 페이지 및 OAuth2 로그인을 위한 보안 설정 (Stateful, 세션 사용)
  @Bean
  @Order(2)
  public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // "/", "/oauth2/**" 등 로그인 관련 경로는 모두 허용
            .requestMatchers("/", "/login/**", "/oauth2/**", "/error").permitAll()
            .anyRequest().authenticated() // 그 외의 웹 경로는 인증 필요 (필요시 denyAll() 또는 다른 규칙 적용)
        )
        .oauth2Login(oauth -> oauth
            // OAuth2 로그인 성공 후 사용자 정보를 가져올 때의 설정
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(customOAuth2UserService) // OIDC 기반의 UserService 등록
            )
            .successHandler(oAuth2LoginSuccessHandler) // 로그인 성공 시 핸들러
            .failureHandler(oAuth2AuthenticationFailureHandler) // 로그인 실패 시 핸들러
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // OAuth2 로그인을 위해 필요 시 세션 생성
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .logout(logout -> logout
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
        );

    return http.build();
  }
}
