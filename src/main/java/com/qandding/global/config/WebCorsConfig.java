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

    // ⚠️ 모든 출처(Origin)를 허용합니다. (보안에 매우 취약)
    config.addAllowedOriginPattern("*");

    // 모든 HTTP Method(GET, POST 등)를 허용합니다.
    config.setAllowedMethods(List.of("*"));

    // 모든 HTTP Header를 허용합니다.
    config.setAllowedHeaders(List.of("*"));

    // 자격 증명(쿠키, 인증 헤더 등)을 허용합니다.
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 모든 경로에 대해 위 설정을 적용합니다.
    source.registerCorsConfiguration("/**", config);

    return source;
  }
}