package com.qandding.global.auth.aspect;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.auth.JwtTokenValidator;
import com.qandding.global.auth.annotation.JwtTokenRequired;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 검증을 위한 AOP 어드바이스
 * @JwtTokenRequired 어노테이션이 붙은 메서드 실행 전에 토큰 검증을 수행
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class JwtTokenValidationAspect {

    private final JwtTokenValidator jwtTokenValidator;

    /**
     * @JwtTokenRequired 어노테이션이 붙은 메서드 실행 전에 JWT 토큰 검증 수행
     * @param joinPoint 실행되는 메서드 정보
     * @param jwtTokenRequired 어노테이션 정보
     */
    @Before("@annotation(jwtTokenRequired)")
    public void validateJwtToken(JoinPoint joinPoint, JwtTokenRequired jwtTokenRequired) {
        try {
            // Spring Security 컨텍스트에서 인증 정보 가져오기
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("인증되지 않은 사용자의 요청 - method: {}", joinPoint.getSignature().getName());
                throw new BusinessException(ErrorCode.UNAUTHORIZED, jwtTokenRequired.message());
            }
            
            // CustomUserPrincipal 검증
            if (authentication.getPrincipal() instanceof CustomUserPrincipal customPrincipal) {
                jwtTokenValidator.validateUserPrincipal(customPrincipal);
                log.debug("JWT 토큰 검증 완료 - method: {}, userId: {}", 
                        joinPoint.getSignature().getName(), customPrincipal.getUserId());
            } else {
                log.error("잘못된 인증 정보 타입 - method: {}, principalType: {}", 
                        joinPoint.getSignature().getName(), 
                        authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "잘못된 인증 정보입니다.");
            }
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류 발생 - method: {}", joinPoint.getSignature().getName(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "토큰 검증 중 오류가 발생했습니다.");
        }
    }
}
