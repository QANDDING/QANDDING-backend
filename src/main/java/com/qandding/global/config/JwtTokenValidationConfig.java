package com.qandding.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JWT 토큰 검증 인터셉터 설정
 * 현재는 JWT 인증 필터만 사용하므로 인터셉터 등록을 제거
 */
@Configuration
@RequiredArgsConstructor
public class JwtTokenValidationConfig implements WebMvcConfigurer {

    // JWT 인증 필터만 사용하므로 인터셉터 등록 제거
    // JwtAuthenticationFilter에서 모든 JWT 토큰 검증을 처리
    
}
