package com.qandding.domain.user.controller;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.service.UserService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {
	private final UserService userService;

    // 회원가입 POST는 사용하지 않음 (OAuth만 사용)

	public record UpdateProfileRequest(
			@NotBlank String nickname,
			@NotBlank String grade,	
			@NotBlank String major
	) {}

    public record CompleteProfileRequest(
            @NotBlank String nickname,
            @NotBlank String grade,
            @NotBlank String major,
            @NotBlank @Email String email
    ) {}

    // POST /api/users 회원가입은 제거

	@GetMapping("/me")
	@Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
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
	@Operation(summary = "내 정보 부분 업데이트", description = "현재 로그인된 사용자의 닉네임, 학년, 전공을 부분 업데이트합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "업데이트 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
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

	@PutMapping("/complete-profile")
	@Operation(summary = "프로필 정보 완성", description = "최초 로그인 후 사용자 프로필 정보를 완성합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "프로필 완성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
    public ResponseEntity<User> completeProfile(@Valid @RequestBody CompleteProfileRequest req) {
		log.info("=== /api/users/complete-profile PUT 호출됨 ===");
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			log.warn("인증되지 않은 사용자");
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (principal instanceof CustomUserPrincipal customPrincipal) {
			log.info("프로필 완성 요청. userId: {}", customPrincipal.getUserId());
            User updated = userService.completeUserProfile(customPrincipal.getUserId(), req.nickname(), req.grade(), req.major(), req.email());
            return ResponseEntity.ok(updated);
		} else {
			log.warn("Principal 타입 불일치");
			return ResponseEntity.status(401).build();
		}
	}

	@DeleteMapping("/me")
	@Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자를 탈퇴 처리합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "탈퇴 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
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
