package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)
                    && jwtTokenProvider.isAccessToken(jwt) && tokenService.isTokenValid(jwt)) {
                
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                
                userRepository.findById(userId).ifPresent(user -> {
                    CustomUserPrincipal userPrincipal = new CustomUserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        user.getNickname(),
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT認証成功 - userId: {}", userId);
                });
            }
        } catch (Exception e) {
            log.error("JwtAuthenticationFilterで例外発生", e);
        }
        
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}