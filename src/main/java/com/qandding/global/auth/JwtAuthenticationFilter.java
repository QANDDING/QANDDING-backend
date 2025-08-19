package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository, TokenService tokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Access Token인지 확인
                if (!jwtTokenProvider.isAccessToken(jwt)) {
                    logger.warn("Access Token이 아닌 토큰으로 요청: " + jwt.substring(0, 20) + "...");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 토큰 유효성 체크 (DB에 존재하고 만료되지 않았는지)
                if (!tokenService.isTokenValid(jwt)) {
                    logger.warn("유효하지 않은 토큰: " + jwt.substring(0, 20) + "...");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                
                User user = userRepository.findById(userId).orElse(null);
                
                if (user != null && user.getEmail().equals(email)) {
                    CustomUserPrincipal userPrincipal = new CustomUserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        user.getNickname(),
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, userPrincipal.getAuthorities()
                        );
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("JWT 인증 성공 - userId: " + userId + ", email: " + email);
                } else {
                    logger.warn("JWT 토큰은 유효하지만 사용자 정보를 찾을 수 없음 - userId: " + userId + ", email: " + email);
                }
            } else {
                if (StringUtils.hasText(jwt)) {
                    logger.debug("JWT 토큰이 유효하지 않음: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");
                }
            }
        } catch (Exception e) {
            logger.error("JWT 토큰 처리 중 오류 발생", e);
        }
        
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더에서 Bearer 토큰 추출 (우선순위)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 2. 쿠키에서 access_token 추출 (하위 호환성)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}
