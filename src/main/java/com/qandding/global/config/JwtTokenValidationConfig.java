package com.qandding.global.config;

import com.qandding.global.auth.interceptor.JwtTokenValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JWT 토큰 검증 인터셉터 설정
 * 특정 경로에 대해 JWT 토큰 검증을 수행하는 인터셉터를 등록
 */
@Configuration
@RequiredArgsConstructor
public class JwtTokenValidationConfig implements WebMvcConfigurer {

    private final JwtTokenValidationInterceptor jwtTokenValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenValidationInterceptor)
                .addPathPatterns(
                        "/api/ai/**",           // AI 관련 API
                        "/api/users/**",         // 사용자 관련 API
                        "/api/comments/**",      // 댓글 관련 API
                        "/api/ai-answers/**",    // AI 답변 관련 API
                        "/api/questions/**",     // 질문 관련 API
                        "/api/user-answers/**",  // 사용자 답변 관련 API
                        "/api/answers/**",       // 답변 관련 API
                        "/api/storage/**"        // 스토리지 관련 API
                )
                .excludePathPatterns(
                        "/api/auth/**",          // 인증 관련 API는 제외
                        "/api/health",           // 헬스 체크는 제외
                        "/",                     // 루트 경로는 제외
                        "/error",                // 에러 페이지는 제외
                        "/swagger-ui/**",        // Swagger UI는 제외
                        "/v3/api-docs/**",       // API 문서는 제외
                        "/favicon.ico"           // 파비콘은 제외
                );
    }
}
