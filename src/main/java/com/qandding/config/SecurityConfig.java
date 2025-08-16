package com.qandding.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qandding.security.JwtCookieAuthenticationFilter;
import com.qandding.security.RefreshTokenService;
import com.qandding.security.RateLimitFilter;
import com.qandding.security.TokenBlacklistService;
import com.qandding.user.domain.User;
import com.qandding.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Value("${app.oauth.allowed-domain}")
	private String allowedDomain;

	@Value("${app.oauth.redirect-url}")
	private String redirectUrl;

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

	private final UserRepository userRepository;
	private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;
	private final CorsConfigurationSource corsConfigurationSource;
	private final RefreshTokenService refreshTokenService;
	private final RateLimitFilter rateLimitFilter;

	public SecurityConfig(UserRepository userRepository,
	                     JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
	                     CorsConfigurationSource corsConfigurationSource,
	                     RefreshTokenService refreshTokenService,
	                     RateLimitFilter rateLimitFilter) {
		this.userRepository = userRepository;
		this.jwtCookieAuthenticationFilter = jwtCookieAuthenticationFilter;
		this.corsConfigurationSource = corsConfigurationSource;
		this.refreshTokenService = refreshTokenService;
		this.rateLimitFilter = rateLimitFilter;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.headers(headers -> headers
				.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
				.frameOptions(frame -> frame.sameOrigin())
				.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/api/health", "/login", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth -> oauth
				.userInfoEndpoint(userInfo -> userInfo
					.oidcUserService(oidcUserService())
					.userService(oAuth2UserService())
				)
				.successHandler(oauth2SuccessHandler())
			)
			.logout(Customizer.withDefaults())
			.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
		OidcUserService delegate = new OidcUserService();
		return userRequest -> {
			OidcUser oidcUser = delegate.loadUser(userRequest);
			String email = oidcUser.getEmail();
			validateDomain(email);
			return oidcUser;
		};
	}

	private OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
		DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
		return userRequest -> {
			OAuth2User user = delegate.loadUser(userRequest);
			String email = (String) user.getAttributes().getOrDefault("email", "");
			validateDomain(email);
			return new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("ROLE_USER")),
				(Map<String, Object>) user.getAttributes(),
				"email"
			);
		};
	}

	private void validateDomain(String email) {
		if (email == null || !email.endsWith("@" + allowedDomain)) {
			throw new RuntimeException("허용되지 않은 이메일 도메인입니다.");
		}
	}

	@Bean
	AuthenticationSuccessHandler oauth2SuccessHandler() {
		return (request, response, authentication) -> {
			Object principal = authentication.getPrincipal();
			String email;
			String name;
			if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
				email = oidc.getEmail();
				name = oidc.getFullName();
			} else if (principal instanceof OAuth2User oAuth2) {
				email = (String) oAuth2.getAttributes().getOrDefault("email", "");
				name = (String) oAuth2.getAttributes().getOrDefault("name", "");
			} else {
				throw new IllegalStateException("지원하지 않는 principal");
			}

			User user = userRepository.findByEmail(email)
				.orElseGet(() -> userRepository.save(new User(name, "", "", email)));
			if (!user.isEmailVerified()) {
				user.markEmailVerified();
			}

			boolean needsProfile = (user.getGrade() == null || user.getGrade().isBlank()) || (user.getMajor() == null || user.getMajor().isBlank());

			SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
			Instant now = Instant.now();
			String jti = UUID.randomUUID().toString();
			String accessToken = Jwts.builder()
				.setId(jti)
				.setSubject(String.valueOf(user.getId()))
				.claim("email", user.getEmail())
				.claim("nickname", user.getNickname())
				.setIssuedAt(Date.from(now))
				.setExpiration(Date.from(now.plusSeconds(jwtExpiresMinutes * 60)))
				.signWith(key)
				.compact();

			// Refresh token 발급(회수형)
			String refreshToken = refreshTokenService.issue(user.getId());

			String accessCookie = String.format("%s=%s; Path=%s; Domain=%s; Max-Age=%d; %s; SameSite=%s",
				jwtCookieName, accessToken, jwtCookiePath, jwtCookieDomain, (int)(jwtExpiresMinutes*60),
				(jwtCookieSecure ? "Secure; " : "") + (jwtCookieHttpOnly ? "HttpOnly" : ""), jwtCookieSameSite);
			String refreshCookie = String.format("%s=%s; Path=%s; Domain=%s; Max-Age=%d; %s; SameSite=%s",
				refreshCookieName, refreshToken, refreshCookiePath, refreshCookieDomain, (int)(refreshTtlDays*24*60*60),
				(refreshCookieSecure ? "Secure; " : "") + (refreshCookieHttpOnly ? "HttpOnly" : ""), refreshCookieSameSite);
			response.addHeader("Set-Cookie", accessCookie);
			response.addHeader("Set-Cookie", refreshCookie);

			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			Map<String, Object> body = new HashMap<>();
			body.put("success", true);
			body.put("needsProfile", needsProfile);
			body.put("redirectUrl", redirectUrl);
			new ObjectMapper().writeValue(response.getOutputStream(), body);
		};
	}
}
