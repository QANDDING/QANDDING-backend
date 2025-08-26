package com.qandding.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                                     AuthenticationException exception) throws IOException {
        
        log.error("=== OAuth2 인증 실패 핸들러 시작 ===");
        log.error("요청 URL: {}", request.getRequestURL());
        log.error("요청 메서드: {}", request.getMethod());
        log.error("요청 파라미터: {}", request.getQueryString());
        log.error("User-Agent: {}", request.getHeader("User-Agent"));
        log.error("Referer: {}", request.getHeader("Referer"));
        
        // 예외 정보 상세 로깅
        log.error("예외 타입: {}", exception.getClass().getSimpleName());
        log.error("예외 메시지: {}", exception.getMessage());
        
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauthException = (OAuth2AuthenticationException) exception;
            log.error("OAuth2 예외 상세: {}", oauthException.getError());
            log.error("OAuth2 예외 메시지: {}", oauthException.getMessage());
        }
        
        // 스택 트레이스 로깅
        log.error("스택 트레이스:", exception);
        
        // 실패 원인 분석
        String errorReason = analyzeFailureReason(exception);
        log.error("실패 원인 분석: {}", errorReason);
        
        // 프론트엔드로 오류 정보 전달
        try {
            String errorUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("success", "false")
                    .queryParam("error", errorReason)
                    .queryParam("errorType", exception.getClass().getSimpleName())
                    .build().toUriString();
            
            log.info("오류 페이지로 리다이렉트: {}", errorUrl);
            response.sendRedirect(errorUrl);
            
        } catch (Exception redirectError) {
            log.error("오류 페이지 리다이렉트 실패", redirectError);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("OAuth2 인증 실패: " + errorReason);
        }
        
        log.error("=== OAuth2 인증 실패 핸들러 완료 ===");
    }
    
    private String analyzeFailureReason(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauthException = (OAuth2AuthenticationException) exception;
            String error = oauthException.getError().getErrorCode();
            
            switch (error) {
                case "access_denied":
                    return "사용자가 OAuth2 인증을 취소했습니다.";
                case "invalid_request":
                    return "OAuth2 요청이 잘못되었습니다.";
                case "unauthorized_client":
                    return "클라이언트가 OAuth2 인증을 받을 권한이 없습니다.";
                case "unsupported_response_type":
                    return "지원되지 않는 응답 타입입니다.";
                case "invalid_scope":
                    return "요청된 스코프가 유효하지 않습니다.";
                case "server_error":
                    return "OAuth2 서버 오류가 발생했습니다.";
                case "temporarily_unavailable":
                    return "OAuth2 서비스가 일시적으로 사용할 수 없습니다.";
                default:
                    return "OAuth2 인증 실패: " + error;
            }
        }
        
        // 일반적인 인증 실패 원인 분석
        String message = exception.getMessage();
        if (message != null) {
            if (message.contains("허용되지 않은 이메일 도메인")) {
                return "허용되지 않은 이메일 도메인입니다.";
            } else if (message.contains("사용자를 찾을 수 없음")) {
                return "사용자 정보를 찾을 수 없습니다.";
            } else if (message.contains("데이터베이스")) {
                return "데이터베이스 연결 오류가 발생했습니다.";
            }
        }
        
        return "알 수 없는 인증 오류가 발생했습니다.";
    }
}
