package com.qandding.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qandding.global.common.error.ApiErrorResponse;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
	private final StringRedisTemplate redis;
	private final boolean enabled;
	private final long requests;
	private final long seconds;

	public RateLimitFilter(
			StringRedisTemplate redis,
			@Value("${app.ratelimit.enabled:true}") boolean enabled,
			@Value("${app.ratelimit.requests:100}") long requests,
			@Value("${app.ratelimit.seconds:60}") long seconds
	) {
		this.redis = redis;
		this.enabled = enabled;
		this.requests = requests;
		this.seconds = seconds;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!enabled) {
			filterChain.doFilter(request, response);
			return;
		}

		String key = buildKey(request);
		try {
			Long count = redis.opsForValue().increment(key);
			if (count != null && count == 1L) {
				redis.expire(key, java.time.Duration.ofSeconds(seconds));
			}
			if (count != null && count > requests) {
				respondTooManyRequests(response);
				return;
			}
		} catch (Exception ignore) {
			// Redis 장애 시 레이트리밋은 우회
		}
		filterChain.doFilter(request, response);
	}

	private String buildKey(HttpServletRequest request) {
		Object principal = request.getUserPrincipal();
		if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token
				&& token.getPrincipal() instanceof CustomUserPrincipal p) {
			return "rl:user:" + p.getUserId();
		}
		String ip = getClientIp(request);
		return "rl:ip:" + ip;
	}

	private String getClientIp(HttpServletRequest request) {
		String h = request.getHeader("X-Forwarded-For");
		if (h != null && !h.isBlank()) {
			int comma = h.indexOf(',');
			return comma > 0 ? h.substring(0, comma).trim() : h.trim();
		}
		return request.getRemoteAddr();
	}

	private void respondTooManyRequests(HttpServletResponse response) throws IOException {
		response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		ApiErrorResponse body = ApiErrorResponse.of(ErrorCode.RATE_LIMIT_EXCEEDED, ErrorCode.RATE_LIMIT_EXCEEDED.getDefaultMessage());
		new ObjectMapper().writeValue(response.getOutputStream(), body);
	}
}
