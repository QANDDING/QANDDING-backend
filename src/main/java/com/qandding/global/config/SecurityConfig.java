package com.qandding.global.config;

import com.qandding.global.auth.CustomOAuth2UserService;
import com.qandding.global.auth.JwtAuthenticationFilter;
import com.qandding.global.auth.OAuth2AuthenticationFailureHandler;
import com.qandding.global.auth.OAuth2LoginSuccessHandler;
import com.qandding.global.ratelimit.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final RateLimitFilter rateLimitFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        log.info("=== API SecurityFilterChain 설정 시작 ===");
        
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .oauth2Login(AbstractHttpConfigurer::disable)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("=== API SecurityFilterChain 설정 완료 ===");
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        log.info("=== Web SecurityFilterChain 설정 시작 ===");
        log.info("CustomOAuth2UserService: {}", customOAuth2UserService.getClass().getSimpleName());
        log.info("OAuth2LoginSuccessHandler: {}", oAuth2LoginSuccessHandler.getClass().getSimpleName());
        log.info("OAuth2AuthenticationFailureHandler: {}", oAuth2AuthenticationFailureHandler.getClass().getSimpleName());
        
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login/**", "/oauth2/**", "/error", "/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico").permitAll()
                .anyRequest().denyAll()
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> {
                    log.info("OAuth2 userInfoEndpoint 설정");
                    userInfo.userService(customOAuth2UserService);
                })
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .logout(logout -> logout.logoutSuccessUrl("/"));

        log.info("=== Web SecurityFilterChain 설정 완료 ===");
        return http.build();
    }
}