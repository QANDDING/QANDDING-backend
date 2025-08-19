package com.qandding.global.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JWT 토큰 검증이 필요한 메서드에 사용하는 어노테이션
 * 이 어노테이션이 붙은 메서드는 반드시 유효한 JWT 토큰이 필요함
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JwtTokenRequired {
    
    /**
     * 토큰 검증 실패 시 반환할 에러 메시지
     */
    String message() default "인증이 필요합니다.";
    
    /**
     * 토큰 검증 실패 시 반환할 에러 코드
     */
    String errorCode() default "UNAUTHORIZED";
}
