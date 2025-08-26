package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Value("${app.oauth.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        try {
            log.info("=== OAuth2 로그인 성공 핸들러 시작 ===");
            log.info("요청 URL: {}", request.getRequestURL());
            log.info("요청 메서드: {}", request.getMethod());
            log.info("리다이렉트 URL 설정값: {}", redirectUrl);
            
            // Authentication 객체 정보 로깅
            log.info("Authentication 객체 타입: {}", authentication.getClass().getSimpleName());
            log.info("Authentication Principal 타입: {}", authentication.getPrincipal().getClass().getSimpleName());
            log.info("Authentication Authorities: {}", authentication.getAuthorities());
            
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            log.info("OAuth2User 객체 생성 성공");
            
            // OAuth2User 속성들 로깅
            log.info("OAuth2User Attributes 전체: {}", oAuth2User.getAttributes());
            
            String email = (String) oAuth2User.getAttributes().get("email");
            log.info("추출된 이메일: {}", email);
            
            if (email == null || email.isBlank()) {
                log.error("이메일이 null이거나 비어있음");
                throw new IllegalArgumentException("OAuth2에서 이메일을 가져올 수 없습니다.");
            }
            
            // 사용자 조회 로깅
            log.info("데이터베이스에서 사용자 조회 시작: {}", email);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("이메일로 사용자를 찾을 수 없음: {}", email);
                        return new IllegalArgumentException("OAuth2 인증 후 사용자를 찾을 수 없습니다.");
                    });
            log.info("사용자 조회 성공: ID={}, 닉네임={}", user.getId(), user.getNickname());
            
            // 프로필 완성도 확인
            boolean needsProfile = (user.getGrade() == null || user.getGrade().isBlank()) ||
                                   (user.getMajor() == null || user.getMajor().isBlank());
            log.info("프로필 완성도 확인: needsProfile={}, grade={}, major={}", 
                    needsProfile, user.getGrade(), user.getMajor());
            
            // JWT 토큰 생성
            log.info("JWT 토큰 생성 시작");
            TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user);
            log.info("JWT 토큰 생성 성공: accessToken 길이={}, refreshToken 길이={}", 
                    tokenPair.getAccessToken().length(), tokenPair.getRefreshToken().length());
            
            // 리다이렉트 URL 생성
            log.info("리다이렉트 URL 생성 시작");
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("success", "true")
                    .queryParam("needsProfile", needsProfile)
                    .queryParam("accessToken", tokenPair.getAccessToken())
                    .queryParam("refreshToken", tokenPair.getRefreshToken())
                    .build().toUriString();
            log.info("생성된 리다이렉트 URL: {}", targetUrl);
            
            // 리다이렉트 실행
            log.info("브라우저 리다이렉트 실행");
            response.sendRedirect(targetUrl);
            log.info("=== OAuth2 로그인 성공 핸들러 완료 ===");
            
        } catch (Exception e) {
            log.error("=== OAuth2 로그인 성공 핸들러에서 오류 발생 ===", e);
            log.error("오류 타입: {}", e.getClass().getSimpleName());
            log.error("오류 메시지: {}", e.getMessage());
            log.error("스택 트레이스:", e);
            
            // 오류 발생 시에도 응답 전송
            try {
                String errorUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                        .queryParam("success", "false")
                        .queryParam("error", e.getMessage())
                        .build().toUriString();
                response.sendRedirect(errorUrl);
                log.info("오류 발생으로 인한 리다이렉트: {}", errorUrl);
            } catch (Exception redirectError) {
                log.error("오류 페이지 리다이렉트 실패", redirectError);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("OAuth2 로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }
}
