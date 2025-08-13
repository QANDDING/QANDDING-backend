package com.qandding.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final RefreshTokenService refreshTokenService;

	@Value("${app.jwt.secret}")
	private String jwtSecret;
	@Value("${app.jwt.expires-minutes}")
	private long jwtExpiresMinutes;
	@Value("${app.jwt.cookie.name}")
	private String jwtCookieName;
	@Value("${app.jwt.cookie.domain}")
	private String jwtCookieDomain;
	@Value("${app.jwt.cookie.path}")
	private String jwtCookiePath;
	@Value("${app.jwt.cookie.secure}")
	private boolean jwtCookieSecure;
	@Value("${app.jwt.cookie.http-only}")
	private boolean jwtCookieHttpOnly;
	@Value("${app.jwt.cookie.same-site}")
	private String jwtCookieSameSite;

	@Value("${app.refresh-token.cookie.name}")
	private String refreshCookieName;
	@Value("${app.refresh-token.cookie.domain}")
	private String refreshCookieDomain;
	@Value("${app.refresh-token.cookie.path}")
	private String refreshCookiePath;
	@Value("${app.refresh-token.cookie.secure}")
	private boolean refreshCookieSecure;
	@Value("${app.refresh-token.cookie.http-only}")
	private boolean refreshCookieHttpOnly;
	@Value("${app.refresh-token.cookie.same-site}")
	private String refreshCookieSameSite;
	@Value("${app.refresh-token.ttl-days}")
	private long refreshTtlDays;

	public AuthController(RefreshTokenService refreshTokenService) {
		this.refreshTokenService = refreshTokenService;
	}

	@PostMapping("/refresh")
	public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
		String refresh = extractCookie(request, refreshCookieName);
		Optional<Long> userIdOpt = refreshTokenService.consume(refresh);
		if (userIdOpt.isEmpty()) {
			return ResponseEntity.status(401).build();
		}
		Long userId = userIdOpt.get();
		// Access 재발급
		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		Instant now = Instant.now();
		String accessToken = Jwts.builder()
			.setId(UUID.randomUUID().toString())
			.setSubject(String.valueOf(userId))
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(now.plusSeconds(jwtExpiresMinutes * 60)))
			.signWith(key)
			.compact();
		// Refresh 로테이션
		String newRefresh = refreshTokenService.issue(userId);

		String accessCookie = String.format("%s=%s; Path=%s; Domain=%s; Max-Age=%d; %s; SameSite=%s",
				jwtCookieName, accessToken, jwtCookiePath, jwtCookieDomain, (int)(jwtExpiresMinutes*60),
				(jwtCookieSecure ? "Secure; " : "") + (jwtCookieHttpOnly ? "HttpOnly" : ""), jwtCookieSameSite);
		String refreshCookie = String.format("%s=%s; Path=%s; Domain=%s; Max-Age=%d; %s; SameSite=%s",
				refreshCookieName, newRefresh, refreshCookiePath, refreshCookieDomain, (int)(refreshTtlDays*24*60*60),
				(refreshCookieSecure ? "Secure; " : "") + (refreshCookieHttpOnly ? "HttpOnly" : ""), refreshCookieSameSite);
		response.addHeader("Set-Cookie", accessCookie);
		response.addHeader("Set-Cookie", refreshCookie);
		return ResponseEntity.noContent().build();
	}

	private String extractCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		for (Cookie c : cookies) {
			if (name.equals(c.getName())) return c.getValue();
		}
		return null;
	}
}
