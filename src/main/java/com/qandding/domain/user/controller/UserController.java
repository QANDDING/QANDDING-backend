package com.qandding.domain.user.controller;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.service.UserService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
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
	public ResponseEntity<User> me() {
		log.info("=== /api/users/me 호출됨 ===");
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		log.info("Authentication 객체 확인됨");
		
		if (authentication == null) {
			log.warn("Authentication이 null입니다.");
			return ResponseEntity.status(401).build();
		}
		
		if (!authentication.isAuthenticated()) {
			log.warn("Authentication이 인증되지 않았습니다.");
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		log.info("Principal 객체 타입: {}", principal != null ? principal.getClass().getSimpleName() : "null");
		
		if (principal instanceof CustomUserPrincipal customPrincipal) {
			log.info("CustomUserPrincipal 확인됨. userId: {}", customPrincipal.getUserId());
			User user = userService.get(customPrincipal.getUserId());
			log.info("사용자 정보 조회 성공");
			return ResponseEntity.ok(user);
		} else {
			log.warn("Principal이 CustomUserPrincipal이 아닙니다. 실제 타입: {}", 
				principal != null ? principal.getClass().getName() : "null");
			return ResponseEntity.status(401).build();
		}
	}

	@PatchMapping("/me")
	public ResponseEntity<User> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
		log.info("=== /api/users/me PATCH 호출됨 ===");
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			log.warn("인증되지 않은 사용자");
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (principal instanceof CustomUserPrincipal customPrincipal) {
			log.info("프로필 업데이트 요청. userId: {}", customPrincipal.getUserId());
			User updated = userService.updateProfile(customPrincipal.getUserId(), req.nickname(), req.grade(), req.major());
			return ResponseEntity.ok(updated);
		} else {
			log.warn("Principal 타입 불일치");
			return ResponseEntity.status(401).build();
		}
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> withdraw() {
		log.info("=== /api/users/me DELETE 호출됨 ===");
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			log.warn("인증되지 않은 사용자");
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (principal instanceof CustomUserPrincipal customPrincipal) {
			log.info("회원 탈퇴 요청. userId: {}", customPrincipal.getUserId());
			userService.delete(customPrincipal.getUserId());
			// Spring Security가 자동으로 세션 무효화 처리
			return ResponseEntity.noContent().build();
		} else {
			log.warn("Principal 타입 불일치");
			return ResponseEntity.status(401).build();
		}
	}
}
