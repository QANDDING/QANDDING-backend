package com.qandding.global.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qandding.domain.user.entity.CustomUserPrincipal;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final TokenService tokenService;
  private final JwtTokenProvider jwtTokenProvider;

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refreshToken(
      @RequestBody RefreshTokenRequest request,
      HttpServletResponse response) {
    try {
      String newAccessToken = tokenService.refreshAccessToken(request.refreshToken());

      // 새로운 Access Token을 httpOnly 쿠키로 설정 (하위 호환성)
      Cookie accessCookie = new Cookie("access_token", newAccessToken);
      accessCookie.setHttpOnly(true);
      accessCookie.setSecure(true);
      accessCookie.setPath("/");
      accessCookie.setMaxAge(900); // 15분
      response.addCookie(accessCookie);

      // 새로운 Access Token을 응답 본문에도 포함 (프론트엔드에서 Authorization 헤더 사용 시)
      return ResponseEntity.ok(new TokenResponse(newAccessToken, null, "ACCESS_TOKEN_REFRESHED"));
    } catch (Exception e) {
      log.error("토큰 재발급 실패: {}", e.getMessage());
      return ResponseEntity.badRequest().body(new TokenResponse(null, null, "REFRESH_FAILED"));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @AuthenticationPrincipal CustomUserPrincipal customPrincipal,
      HttpServletResponse response) {
    try {
      // 사용자가 인증되지 않은 경우에도 쿠키만 만료 처리
      if (customPrincipal == null) {
        log.warn("로그아웃 요청: 인증되지 않은 사용자");
      } else {
        // 현재 사용자의 모든 토큰 무효화
        tokenService.invalidateTokens(customPrincipal.getUserId());
        log.info("사용자 로그아웃 완료 - userId: {}", customPrincipal.getUserId());
      }

      // Access Token 쿠키 만료 처리
      Cookie accessCookie = new Cookie("access_token", "");
      accessCookie.setHttpOnly(true);
      accessCookie.setSecure(true);
      accessCookie.setPath("/");
      accessCookie.setMaxAge(0); // 즉시 만료
      response.addCookie(accessCookie);

    } catch (Exception e) {
      log.error("로그아웃 처리 중 오류: {}", e.getMessage());
    }
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout-token")
  public ResponseEntity<Void> logoutWithToken(@RequestBody LogoutTokenRequest request) {
    try {
      // 특정 토큰을 무효화 (토큰에서 사용자 ID 추출하여 해당 사용자의 모든 토큰 삭제)
      String token = request.token();
      if (token != null && !token.isEmpty()) {
        // JWT 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId != null) {
          tokenService.invalidateTokens(userId);
          log.info("토큰 무효화 완료 - userId: {}", userId);
        }
      }
    } catch (Exception e) {
      log.error("토큰 무효화 중 오류: {}", e.getMessage());
    }
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/check")
  public ResponseEntity<Boolean> checkAuth() {
    // Spring Security가 자동으로 인증 상태 확인
    return ResponseEntity.ok(true);
  }

  @PostMapping("/validate")
  public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request) {
    try {
      String token = request.token();
      if (!StringUtils.hasText(token)) {
        return ResponseEntity.badRequest().body(new TokenValidationResponse(false, "토큰이 제공되지 않았습니다."));
      }

      // 토큰 유효성 검증
      boolean isValid = jwtTokenProvider.validateToken(token) &&
          jwtTokenProvider.isAccessToken(token) &&
          tokenService.isTokenValid(token);

      if (isValid) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        String email = jwtTokenProvider.getEmailFromToken(token);
        return ResponseEntity.ok(new TokenValidationResponse(true, "유효한 토큰입니다.", userId, email));
      } else {
        return ResponseEntity.ok(new TokenValidationResponse(false, "유효하지 않은 토큰입니다."));
      }
    } catch (Exception e) {
      log.error("토큰 검증 중 오류: {}", e.getMessage());
      return ResponseEntity.ok(new TokenValidationResponse(false, "토큰 검증 중 오류가 발생했습니다."));
    }
  }

  // DTO 클래스들
  public record RefreshTokenRequest(String refreshToken) {}

  public record TokenResponse(String accessToken, String refreshToken, String message) {}

  public record LogoutTokenRequest(String token) {}

  public record TokenValidationRequest(String token) {}

  public record TokenValidationResponse(boolean valid, String message, Long userId, String email) {
    public TokenValidationResponse(boolean valid, String message) {
      this(valid, message, null, null);
    }
  }
}
