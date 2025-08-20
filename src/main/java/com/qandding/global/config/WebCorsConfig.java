package com.qandding.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class WebCorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // 허용할 출처(Origin) 목록을 명시적으로 지정합니다.
    config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000",
            "http://localhost:5173",
            "http://127.0.0.1:5173"
            // TODO: 추후 배포 시 실제 프론트엔드 도메인을 추가해야 합니다.
    ));

    // addAllowedOriginPattern("*") 와 setAllowCredentials(true)는 함께 사용될 수 없습니다.
    // config.addAllowedOriginPattern("*");

    // 허용할 HTTP Method를 명시합니다.
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

    // 허용할 HTTP Header를 명시합니다.
    config.setAllowedHeaders(List.of("*"));

    // 자격 증명(쿠키, 인증 헤더 등)을 허용합니다.
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 모든 경로에 대해 위 설정을 적용합니다.
    source.registerCorsConfiguration("/**", config);

    return source;
  }
}