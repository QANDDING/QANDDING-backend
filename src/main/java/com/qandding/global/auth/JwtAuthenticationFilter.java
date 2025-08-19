package com.qandding.global.auth;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.Objects;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final TokenService tokenService;

  // 생성자에서 모든 의존성이 주입되었는지 강제로 확인합니다.
  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository, TokenService tokenService) {
    this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider는 null일 수 없습니다.");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository는 null일 수 없습니다.");
    this.tokenService = Objects.requireNonNull(tokenService, "tokenService는 null일 수 없습니다.");
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String jwt = getJwtFromRequest(request);
      if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)
          && jwtTokenProvider.isAccessToken(jwt) && tokenService.isTokenValid(jwt)) {
        Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
        userRepository.findById(userId).ifPresent(user -> {
          CustomUserPrincipal userPrincipal = new CustomUserPrincipal(
              user.getId(), user.getEmail(), user.getNickname(), List.of(new SimpleGrantedAuthority("ROLE_USER"))
          );
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(authentication);
        });
      }
    } catch (Exception e) {
      log.error("JwtAuthenticationFilter에서 처리되지 않은 예외 발생", e);
      // 여기서 직접 응답을 보내지 않고, 예외를 Security Chain의 후속 처리기(ExceptionTranslationFilter)에 맡깁니다.
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