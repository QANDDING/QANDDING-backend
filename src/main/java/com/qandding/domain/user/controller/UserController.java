package com.qandding.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.service.UserService;
import com.qandding.domain.user.service.UserPostService;
import com.qandding.domain.user.dto.UserPostDtos;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {
	private final UserService userService;
	private final UserPostService userPostService;

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
	public ResponseEntity<User> me(@AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
		// JWT 토큰 검증 (Spring Security가 자동으로 처리)
		if (customPrincipal == null) {
			log.error("인증되지 않은 사용자의 정보 조회 요청");
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		
		log.info("=== /api/users/me 호출됨 ===");
		log.info("CustomUserPrincipal 확인됨. userId: {}", customPrincipal.getUserId());
		
		try {
			User user = userService.get(customPrincipal.getUserId());
			log.info("사용자 정보 조회 성공");
			return ResponseEntity.ok(user);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("사용자 정보 조회 중 오류 발생 - userId: {}", customPrincipal.getUserId(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "사용자 정보 조회 중 오류가 발생했습니다.");
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
	public ResponseEntity<User> updateProfile(
			@Valid @RequestBody UpdateProfileRequest req,
			@AuthenticationPrincipal CustomUserPrincipal customPrincipal
	) {
		// JWT 토큰 검증 (Spring Security가 자동으로 처리)
		if (customPrincipal == null) {
			log.error("인증되지 않은 사용자의 프로필 업데이트 요청");
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		
		log.info("=== /api/users/me PATCH 호출됨 ===");
		log.info("프로필 업데이트 요청. userId: {}", customPrincipal.getUserId());
		
		try {
			User updated = userService.updateProfile(customPrincipal.getUserId(), req.nickname(), req.grade(), req.major());
			log.info("프로필 업데이트 완료 - userId: {}", customPrincipal.getUserId());
			return ResponseEntity.ok(updated);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("프로필 업데이트 중 오류 발생 - userId: {}", customPrincipal.getUserId(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "프로필 업데이트 중 오류가 발생했습니다.");
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
    public ResponseEntity<User> completeProfile(
			@Valid @RequestBody CompleteProfileRequest req,
			@AuthenticationPrincipal CustomUserPrincipal customPrincipal
	) {
		// JWT 토큰 검증 (Spring Security가 자동으로 처리)
		if (customPrincipal == null) {
			log.error("인증되지 않은 사용자의 프로필 완성 요청");
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		
		log.info("=== /api/users/complete-profile PUT 호출됨 ===");
		log.info("프로필 완성 요청. userId: {}", customPrincipal.getUserId());
		
		try {
			User updated = userService.completeUserProfile(customPrincipal.getUserId(), req.nickname(), req.grade(), req.major(), req.email());
			log.info("프로필 완성 완료 - userId: {}", customPrincipal.getUserId());
			return ResponseEntity.ok(updated);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("프로필 완성 중 오류 발생 - userId: {}", customPrincipal.getUserId(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "프로필 완성 중 오류가 발생했습니다.");
		}
	}

	@DeleteMapping("/me")
	@Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자를 탈퇴 처리합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "탈퇴 성공"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
		// JWT 토큰 검증 (Spring Security가 자동으로 처리)
		if (customPrincipal == null) {
			log.error("인증되지 않은 사용자의 회원 탈퇴 요청");
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		
		log.info("=== /api/users/me DELETE 호출됨 ===");
		log.info("회원 탈퇴 요청. userId: {}", customPrincipal.getUserId());
		
		try {
			userService.delete(customPrincipal.getUserId());
			log.info("회원 탈퇴 완료 - userId: {}", customPrincipal.getUserId());
			// Spring Security가 자동으로 세션 무효화 처리
			return ResponseEntity.noContent().build();
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("회원 탈퇴 중 오류 발생 - userId: {}", customPrincipal.getUserId(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "회원 탈퇴 중 오류가 발생했습니다.");
		}
	}

	@GetMapping("/posts")
	@Operation(summary = "내가 쓴 글 보기", description = "현재 로그인된 사용자가 작성한 질문글과 답변글을 페이징하여 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 (페이지 번호가 음수인 경우)"),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "서버 내부 오류")
	})
	public ResponseEntity<UserPostDtos.UserPostsResponse> getMyPosts(
			@RequestParam(defaultValue = "0") int page,
			@AuthenticationPrincipal CustomUserPrincipal customPrincipal
	) {
		// JWT 토큰 검증 (Spring Security가 자동으로 처리)
		if (customPrincipal == null) {
			log.error("인증되지 않은 사용자의 글 조회 요청");
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		
		// 페이지 번호 검증
		if (page < 0) {
			log.error("잘못된 페이지 번호 요청 - page: {}", page);
			throw new BusinessException(ErrorCode.BAD_REQUEST, "페이지 번호는 0 이상이어야 합니다.");
		}
		
		log.info("=== /api/users/posts 호출됨 ===");
		log.info("내가 쓴 글 조회 요청. userId: {}, page: {}", customPrincipal.getUserId(), page);
		
		try {
			UserPostDtos.UserPostsResponse response = userPostService.getUserPosts(customPrincipal.getUserId(), page, 10);
			log.info("내가 쓴 글 조회 완료 - userId: {}, 질문글: {}개, 답변글: {}개", 
					customPrincipal.getUserId(), response.getQuestions().size(), response.getAnswers().size());
			return ResponseEntity.ok(response);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("내가 쓴 글 조회 중 오류 발생 - userId: {}", customPrincipal.getUserId(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "내가 쓴 글 조회 중 오류가 발생했습니다.");
		}
	}
}
