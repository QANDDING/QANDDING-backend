package com.qandding.user.presentation;

import com.qandding.security.CustomUserPrincipal;
import com.qandding.security.TokenBlacklistService;
import com.qandding.user.domain.User;
import com.qandding.user.service.UserService;
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

	@GetMapping("/{id}")
	public ResponseEntity<User> get(@PathVariable Long id) {
		return ResponseEntity.ok(userService.get(id));
	}

	@GetMapping
	public ResponseEntity<List<User>> list() {
		return ResponseEntity.ok(userService.list());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

	private void addExpiredJwtCookie(HttpServletResponse response) {
		String cookie = String.format("%s=; Path=%s; Domain=%s; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; %s; SameSite=%s",
			jwtCookieName, jwtCookiePath, jwtCookieDomain,
			(jwtCookieSecure ? "Secure; " : "") + (jwtCookieHttpOnly ? "HttpOnly" : ""), jwtCookieSameSite);
		response.addHeader("Set-Cookie", cookie);
	}

	private void blacklistCurrentToken(HttpServletRequest request) {
		String token = extractJwtFromCookie(request);
		if (token == null || token.isBlank()) return;
		try {
			SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
			Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
			String jti = claims.getId();
			Date exp = claims.getExpiration();
			if (jti != null && exp != null) {
				long secondsLeft = Math.max(1, (exp.getTime() - System.currentTimeMillis()) / 1000);
				tokenBlacklistService.blacklist(jti, secondsLeft);
			}
		} catch (Exception ignore) {
			// 토큰 파싱 실패 시 무시하고 쿠키만 만료
		}
	}

	private String extractJwtFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		return Arrays.stream(cookies)
			.filter(c -> jwtCookieName.equals(c.getName()))
			.map(Cookie::getValue)
			.findFirst()
			.orElse(null);
	}
}
