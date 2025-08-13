package com.qandding.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
	@Value("${app.jwt.secret}")
	private String jwtSecret;
	@Value("${app.jwt.cookie.name}")
	private String jwtCookieName;

	private final TokenBlacklistService tokenBlacklistService;

	public JwtCookieAuthenticationFilter(TokenBlacklistService tokenBlacklistService) {
		this.tokenBlacklistService = tokenBlacklistService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = resolveBearerToken(request);
		if (token == null) {
			token = resolveCookieToken(request);
		}
		if (token != null) {
			authenticate(token);
		}
		filterChain.doFilter(request, response);
	}

	private String resolveBearerToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}

	private String resolveCookieToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		return Arrays.stream(cookies)
			.filter(c -> jwtCookieName.equals(c.getName()))
			.map(Cookie::getValue)
			.findFirst().orElse(null);
	}

	private void authenticate(String token) {
		try {
			SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
			Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			Claims claims = jws.getBody();
			String jti = claims.getId();
			if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
				SecurityContextHolder.clearContext();
				return;
			}
			Long userId = Long.valueOf(claims.getSubject());
			String email = claims.get("email", String.class);
			String nickname = claims.get("nickname", String.class);
			CustomUserPrincipal principal = new CustomUserPrincipal(
				userId, email, nickname, List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);
			UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (Exception ignored) {
			SecurityContextHolder.clearContext();
		}
	}
}
