package com.qandding.global.auth.jwt;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
	private final StringRedisTemplate redis;
	private final long ttlDays;

	public RefreshTokenService(StringRedisTemplate redis,
	                          @Value("${app.refresh-token.ttl-days}") long ttlDays) {
		this.redis = redis;
		this.ttlDays = ttlDays;
	}

	public String issue(Long userId) {
		String token = UUID.randomUUID().toString();
		String key = key(token);
		redis.opsForValue().set(key, String.valueOf(userId), Duration.ofDays(ttlDays));
		return token;
	}

	public Optional<Long> consume(String token) {
		if (token == null || token.isBlank()) return Optional.empty();
		String key = key(token);
		String userId = redis.opsForValue().get(key);
		if (userId == null) return Optional.empty();
		// 회수(로테이션)
		redis.delete(key);
		return Optional.of(Long.valueOf(userId));
	}

	private String key(String token) {
		return "jwt:refresh:" + token;
	}
}
