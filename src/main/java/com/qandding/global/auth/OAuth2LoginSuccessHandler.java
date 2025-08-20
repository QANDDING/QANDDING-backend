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
        log.info("OAuth2 로그인 성공 핸들러 시작");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("OAuth2 인증 후 사용자를 찾을 수 없습니다."));

        boolean needsProfile = (user.getGrade() == null || user.getGrade().isBlank()) ||
                               (user.getMajor() == null || user.getMajor().isBlank());

        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user);
        log.info("JWT 토큰이 생성되었습니다: {}", user.getEmail());

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("success", "true")
                .queryParam("needsProfile", needsProfile)
                .queryParam("accessToken", tokenPair.getAccessToken())
                .queryParam("refreshToken", tokenPair.getRefreshToken())
                .build().toUriString();

        response.sendRedirect(targetUrl);
        log.info("OAuth2 로그인 성공 핸들러 완료, 리다이렉트 URL: {}", targetUrl);
    }
}
