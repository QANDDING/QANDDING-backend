package com.qandding.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class WebCorsConfig {

  @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173}")
  private String allowedOrigins;

  @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
  private String allowedMethods;

  @Value("${app.cors.allowed-headers:*}")
  private String allowedHeaders;

  @Value("${app.cors.exposed-headers:}")
  private String exposedHeaders;

  @Value("${app.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Value("${app.cors.max-age-seconds:3600}")
  private long maxAgeSeconds;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // 허용할 출처(Origin) 목록을 환경 변수에서 가져옵니다.
    List<String> origins = List.of(allowedOrigins.split(","));
    config.setAllowedOrigins(origins);

    // 허용할 HTTP Method를 환경 변수에서 가져옵니다.
    List<String> methods = List.of(allowedMethods.split(","));
    config.setAllowedMethods(methods);

    // 허용할 HTTP Header를 환경 변수에서 가져옵니다.
    if ("*".equals(allowedHeaders)) {
      config.setAllowedHeaders(List.of("*"));
    } else {
      config.setAllowedHeaders(List.of(allowedHeaders.split(",")));
    }

    // 노출할 HTTP Header를 설정합니다.
    if (exposedHeaders != null && !exposedHeaders.isEmpty()) {
      config.setExposedHeaders(List.of(exposedHeaders.split(",")));
    }

    // 자격 증명(쿠키, 인증 헤더 등)을 허용합니다.
    config.setAllowCredentials(allowCredentials);

    // preflight 요청의 캐시 시간을 설정합니다.
    config.setMaxAge(maxAgeSeconds);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 모든 경로에 대해 위 설정을 적용합니다.
    source.registerCorsConfiguration("/**", config);

    return source;
  }
}