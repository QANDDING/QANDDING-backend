package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final TokenService tokenService;

  @Value("${app.oauth.redirect-url}")
  private String redirectUrl;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    log.info("OAuth2 로그인 성공. JWT 발급을 시작합니다.");

    CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
    User user = customOAuth2User.getUser();

    TokenService.TokenPair tokenPair = tokenService.generateTokenPair(user);
    String accessToken = tokenPair.getAccessToken();
    String refreshToken = tokenPair.getRefreshToken();

    log.info("Access Token, Refresh Token 발급 및 저장 성공");

    boolean needsProfile = (user.getGrade() == null || user.getGrade().isBlank());

    String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
        .queryParam("accessToken", accessToken)
        .queryParam("refreshToken", refreshToken)
        .queryParam("needsProfile", needsProfile)
        .build().toUriString();

    response.sendRedirect(targetUrl);
  }
}
