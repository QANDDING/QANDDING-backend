package com.qandding.global.ratelimit;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
	private final boolean enabled;
	private final long requests;
	private final long seconds;
	
	// 메모리 기반 레이트리밋 카운터
	private final ConcurrentHashMap<String, AtomicLong> memoryCounters = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> memoryTimestamps = new ConcurrentHashMap<>();

	public RateLimitFilter(
			@Value("${app.ratelimit.enabled:true}") boolean enabled,
			@Value("${app.ratelimit.requests:100}") long requests,
			@Value("${app.ratelimit.seconds:60}") long seconds
	) {
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
		if (isRateLimitExceededInMemory(key)) {
			respondTooManyRequests(response);
			return;
		}
		
		filterChain.doFilter(request, response);
	}

	private boolean isRateLimitExceededInMemory(String key) {
		long currentTime = System.currentTimeMillis();
		long windowStart = currentTime - (seconds * 1000);
		
		// 오래된 데이터 정리
		memoryTimestamps.entrySet().removeIf(entry -> entry.getValue() < windowStart);
		memoryCounters.entrySet().removeIf(entry -> !memoryTimestamps.containsKey(entry.getKey()));
		
		// 현재 키에 대한 카운터 가져오기 또는 생성
		AtomicLong counter = memoryCounters.computeIfAbsent(key, k -> new AtomicLong(0));
		memoryTimestamps.put(key, currentTime);
		
		long count = counter.incrementAndGet();
		return count > requests;
	}

	private String buildKey(HttpServletRequest request) {
		Object principal = request.getUserPrincipal();
		if (principal instanceof CustomUserPrincipal customPrincipal) {
			return "rl:user:" + customPrincipal.getUserId();
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
