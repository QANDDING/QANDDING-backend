package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 검증을 위한 헬퍼 클래스
 * 컨트롤러에서 JWT 토큰 검증 로직을 중앙화하여 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenValidator {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;

    /**
     * CustomUserPrincipal이 유효한지 검증
     * @param customPrincipal 검증할 CustomUserPrincipal
     * @throws BusinessException 인증되지 않은 경우
     */
    public void validateUserPrincipal(CustomUserPrincipal customPrincipal) {
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.debug("사용자 인증 검증 완료 - userId: {}", customPrincipal.getUserId());
    }

    /**
     * JWT 토큰이 유효한지 검증
     * @param token 검증할 JWT 토큰
     * @return 검증된 토큰의 사용자 ID
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public Long validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.error("토큰이 제공되지 않음");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰이 제공되지 않았습니다.");
        }

        // JWT 토큰 형식 검증
        if (!jwtTokenProvider.validateToken(token)) {
            log.error("유효하지 않은 JWT 토큰 형식");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        // Access Token인지 확인
        if (!jwtTokenProvider.isAccessToken(token)) {
            log.error("Access Token이 아닌 토큰 사용");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Access Token이 필요합니다.");
        }

        // DB에서 토큰 유효성 확인
        if (!tokenService.isTokenValid(token)) {
            log.error("DB에 저장되지 않은 토큰 또는 만료된 토큰");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰이 만료되었거나 유효하지 않습니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId == null) {
            log.error("토큰에서 사용자 ID를 추출할 수 없음");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰에서 사용자 정보를 추출할 수 없습니다.");
        }

        log.debug("JWT 토큰 검증 완료 - userId: {}", userId);
        return userId;
    }

    /**
     * 사용자 ID가 요청한 사용자와 일치하는지 검증
     * @param customPrincipal 현재 인증된 사용자
     * @param requestedUserId 요청된 사용자 ID
     * @throws BusinessException 권한이 없는 경우
     */
    public void validateUserAccess(CustomUserPrincipal customPrincipal, Long requestedUserId) {
        validateUserPrincipal(customPrincipal);
        
        if (!customPrincipal.getUserId().equals(requestedUserId)) {
            log.error("사용자 접근 권한 없음 - authenticatedUserId: {}, requestedUserId: {}", 
                    customPrincipal.getUserId(), requestedUserId);
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "해당 사용자 정보에 접근할 권한이 없습니다.");
        }
        
        log.debug("사용자 접근 권한 검증 완료 - userId: {}", requestedUserId);
    }
}
