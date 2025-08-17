package com.qandding.global.auth.jwt;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
	private final StringRedisTemplate redis;
	private final boolean enabled;

	public TokenBlacklistService(StringRedisTemplate redis,
	                             @Value("${app.token-blacklist.enabled:true}") boolean enabled) {
		this.redis = redis;
		this.enabled = enabled;
	}

	public void blacklist(String jti, long expiresSeconds) {
		if (!enabled) return;
		redis.opsForValue().set(key(jti), "1", Duration.ofSeconds(expiresSeconds));
	}

	public boolean isBlacklisted(String jti) {
		if (!enabled) return false;
		return Boolean.TRUE.equals(redis.hasKey(key(jti)));
	}

	private String key(String jti) {
		return "jwt:blacklist:" + jti;
	}
}
