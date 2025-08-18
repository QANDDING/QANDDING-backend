package com.qandding.global.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		// Spring Security가 자동으로 세션 무효화 처리
		// 별도의 로직 불필요
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/check")
	public ResponseEntity<Boolean> checkAuth() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
		return ResponseEntity.ok(isAuthenticated);
	}
}
