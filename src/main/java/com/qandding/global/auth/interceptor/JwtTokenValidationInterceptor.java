package com.qandding.global.auth.interceptor;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.auth.JwtTokenValidator;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 토큰 검증을 위한 인터셉터
 * 특정 경로에 대해 JWT 토큰 검증을 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenValidationInterceptor implements HandlerInterceptor {

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // Spring Security 컨텍스트에서 인증 정보 가져오기
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("인증되지 않은 사용자의 요청 - URI: {}", request.getRequestURI());
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
            }
            
            // CustomUserPrincipal 검증
            if (authentication.getPrincipal() instanceof CustomUserPrincipal customPrincipal) {
                jwtTokenValidator.validateUserPrincipal(customPrincipal);
                log.debug("JWT 토큰 검증 완료 - URI: {}, userId: {}", 
                        request.getRequestURI(), customPrincipal.getUserId());
            } else {
                log.error("잘못된 인증 정보 타입 - URI: {}, principalType: {}", 
                        request.getRequestURI(), 
                        authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "잘못된 인증 정보입니다.");
            }
            
            return true;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류 발생 - URI: {}", request.getRequestURI(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "토큰 검증 중 오류가 발생했습니다.");
        }
    }
}
