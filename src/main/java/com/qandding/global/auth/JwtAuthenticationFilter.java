package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
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

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    try {
      // 1. 요청 헤더에서 JWT 토큰을 추출합니다.
      String token = extractTokenFromRequest(request);

      // 2. 토큰이 유효한지 검증합니다.
      if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
        // 3. 토큰에서 사용자 ID를 가져옵니다.
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // 4. 사용자 ID로 DB에서 사용자 정보를 조회합니다.
        userRepository.findById(userId).ifPresent(user -> {
          // 5. 인증을 위한 CustomUserPrincipal 객체를 생성합니다.
          CustomUserPrincipal userPrincipal = new CustomUserPrincipal(
              user.getId(),
              user.getEmail(),
              user.getNickname(),
              List.of(new SimpleGrantedAuthority("ROLE_USER"))
          );

          // 6. Spring Security의 Authentication 객체를 생성하여 SecurityContext에 등록합니다.
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

          SecurityContextHolder.getContext().setAuthentication(authentication);
          log.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", user.getEmail(), request.getRequestURI());
        });
      }
    } catch (Exception e) {
      log.error("Security Context에 사용자 인증 정보를 설정할 수 없습니다.", e);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * HttpServletRequest의 Authorization 헤더에서 Bearer 토큰을 추출합니다.
   * @param request HttpServletRequest 객체
   * @return 추출된 토큰 문자열, 없거나 형식이 맞지 않으면 null
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}