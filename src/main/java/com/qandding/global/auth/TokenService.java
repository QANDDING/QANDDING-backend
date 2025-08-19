package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.entity.UserToken;
import com.qandding.domain.user.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserTokenRepository userTokenRepository;

    public TokenPair generateTokenPair(User user) {
        // 기존 토큰들을 삭제
        deleteAllTokensByUserId(user.getId());
        
        // Access Token 생성 (15분)
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        LocalDateTime accessExpiresAt = LocalDateTime.now().plusMinutes(15);
        UserToken accessTokenEntity = UserToken.createAccessToken(user, accessToken, accessExpiresAt);
        userTokenRepository.save(accessTokenEntity);
        
        // Refresh Token 생성 (7일)
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(7);
        UserToken refreshTokenEntity = UserToken.createRefreshToken(user, refreshToken, refreshExpiresAt);
        userTokenRepository.save(refreshTokenEntity);
        
        log.info("토큰 쌍 생성 완료 - userId: {}, accessToken: {}, refreshToken: {}", 
                user.getId(), accessToken.substring(0, 20) + "...", refreshToken.substring(0, 20) + "...");
        
        return new TokenPair(accessToken, refreshToken);
    }

    public String refreshAccessToken(String refreshToken) {
        try {
            // Refresh Token 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new RuntimeException("유효하지 않은 Refresh Token");
            }

            // DB에서 Refresh Token 확인
            UserToken storedRefreshToken = userTokenRepository.findValidRefreshToken(refreshToken, LocalDateTime.now())
                    .orElseThrow(() -> new RuntimeException("DB에 저장된 Refresh Token과 일치하지 않음"));

            // 사용자 정보 조회
            User user = storedRefreshToken.getUser();
            if (user == null) {
                throw new RuntimeException("사용자 정보를 찾을 수 없음");
            }

            // 기존 토큰들을 모두 삭제
            deleteAllTokensByUserId(user.getId());

            // 새로운 Access Token 생성
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);
            LocalDateTime accessExpiresAt = LocalDateTime.now().plusMinutes(15);
            UserToken newAccessTokenEntity = UserToken.createAccessToken(user, newAccessToken, accessExpiresAt);
            userTokenRepository.save(newAccessTokenEntity);
            
            // 새로운 Refresh Token 생성
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
            LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(7);
            UserToken newRefreshTokenEntity = UserToken.createRefreshToken(user, newRefreshToken, refreshExpiresAt);
            userTokenRepository.save(newRefreshTokenEntity);
            
            log.info("Access Token 재발급 완료 - userId: {}", user.getId());
            
            return newAccessToken;
            
        } catch (Exception e) {
            log.error("Access Token 재발급 실패: {}", e.getMessage());
            throw new RuntimeException("토큰 재발급에 실패했습니다", e);
        }
    }

    public void invalidateTokens(Long userId) {
        // DB에서 사용자의 모든 토큰을 삭제
        deleteAllTokensByUserId(userId);
        
        log.info("사용자 토큰 무효화 완료 - userId: {}", userId);
    }

    public void deleteAllTokensByUserId(Long userId) {
        // 사용자의 모든 토큰을 삭제
        userTokenRepository.deleteAllByUserId(userId);
        log.info("사용자 토큰 삭제 완료 - userId: {}", userId);
    }

    public boolean isTokenValid(String token) {
        // DB에서 토큰이 유효한지 확인 (만료되지 않았고 존재하는지)
        return userTokenRepository.findValidAccessToken(token, LocalDateTime.now()).isPresent();
    }

    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
