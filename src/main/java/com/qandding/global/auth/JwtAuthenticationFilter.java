package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
                // JWT가 없거나 유효하지 않은 경우, OAuth2 세션 확인
                if (request.getSession(false) != null && 
                    request.getSession().getAttribute("OAUTH2_AUTHENTICATED") != null) {
                    logger.debug("OAuth2 인증된 세션 확인됨 - JWT 토큰 발급 필요");
                    // OAuth2 인증은 되어 있지만 JWT 토큰이 없는 경우
                    // 프론트엔드에서 JWT 토큰을 요청하도록 안내
                } else {
                    if (StringUtils.hasText(jwt)) {
                        logger.debug("JWT 토큰이 유효하지 않음: " + jwt.substring(0, Math.min(20, jwt.length())) + "...");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("JWT 토큰 처리 중 오류 발생", e);
        }
        
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Authorization 헤더에서 Bearer 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
