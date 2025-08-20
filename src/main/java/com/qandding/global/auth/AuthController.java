package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    @PostMapping("/refresh")
    @Operation(summary = "Access Token 재발급", description = "유효한 Refresh Token으로 새로운 Access Token과 Refresh Token을 발급받습니다.")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("Access Token 재발급 요청");
        TokenService.TokenPair tokenPair = tokenService.refreshAccessToken(request.refreshToken());
        
        TokenRefreshResponse response = new TokenRefreshResponse(
            tokenPair.getAccessToken(),
            tokenPair.getRefreshToken(),
            "토큰이 성공적으로 재발급되었습니다."
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "서버에 저장된 사용자의 모든 토큰(Access, Refresh)을 무효화합니다.")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        // @AuthenticationPrincipal이 존재한다는 것 자체가 인증되었음을 의미
        log.info("로그아웃 요청 - userId: {}", customPrincipal.getUserId());
        tokenService.invalidateTokens(customPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }
    
    // DTO 정의
    public record RefreshTokenRequest(String refreshToken) {}
    
    public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String message
    ) {}
}