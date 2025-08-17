package com.qandding.domain.user.controller;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.service.UserService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.auth.jwt.TokenBlacklistService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserService userService;
	private final TokenBlacklistService tokenBlacklistService;

	@Value("${app.jwt.cookie.name}")
	private String jwtCookieName;
	@Value("${app.jwt.expires-minutes}")
	private long jwtExpiresMinutes;
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
	@Value("${app.jwt.secret}")
	private String jwtSecret;

	public UserController(UserService userService, TokenBlacklistService tokenBlacklistService) {
		this.userService = userService;
		this.tokenBlacklistService = tokenBlacklistService;
	}

	public record SignUpRequest(
			@NotBlank String nickname,
			@NotBlank String grade,
			@NotBlank String major,
			@NotBlank @Email String email
	) {}

	public record UpdateProfileRequest(
			@NotBlank String nickname,
			@NotBlank String grade,
			@NotBlank String major
	) {}

	@PostMapping
	public ResponseEntity<Long> signUp(@Valid @RequestBody SignUpRequest req) {
		Long id = userService.create(req.nickname(), req.grade(), req.major(), req.email());
		return ResponseEntity.ok(id);
	}

	@GetMapping("/me")
	public ResponseEntity<User> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
		return ResponseEntity.ok(userService.get(principal.getUserId()));
	}

	@PatchMapping("/me")
	public ResponseEntity<User> updateProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                         @Valid @RequestBody UpdateProfileRequest req) {
		User updated = userService.updateProfile(principal.getUserId(), req.nickname(), req.grade(), req.major());
		return ResponseEntity.ok(updated);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		blacklistCurrentToken(request);
		addExpiredJwtCookie(response);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                    HttpServletRequest request,
	                                    HttpServletResponse response) {
		userService.delete(principal.getUserId());
		blacklistCurrentToken(request);
		addExpiredJwtCookie(response);
		return ResponseEntity.noContent().build();
	}

	private void blacklistCurrentToken(HttpServletRequest request) {
		String token = extractJwtToken(request);
		if (token != null) {
			try {
				SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
				Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
				String jti = claims.getId();
				if (jti != null) {
					long expiresIn = claims.getExpiration().getTime() - System.currentTimeMillis();
					if (expiresIn > 0) {
						tokenBlacklistService.blacklist(jti, expiresIn / 1000);
					}
				}
			} catch (Exception ignored) {
				// 토큰 파싱 실패 시 무시
			}
		}
	}

	private void addExpiredJwtCookie(HttpServletResponse response) {
		String expiredCookie = String.format("%s=; Path=%s; Domain=%s; Max-Age=0; %s; SameSite=%s",
				jwtCookieName, jwtCookiePath, jwtCookieDomain,
				(jwtCookieSecure ? "Secure; " : "") + (jwtCookieHttpOnly ? "HttpOnly" : ""), jwtCookieSameSite);
		response.addHeader("Set-Cookie", expiredCookie);
	}

	private String extractJwtToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		return Arrays.stream(cookies)
			.filter(c -> jwtCookieName.equals(c.getName()))
			.map(Cookie::getValue)
			.findFirst().orElse(null);
	}
}
