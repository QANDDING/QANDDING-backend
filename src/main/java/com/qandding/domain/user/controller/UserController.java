package com.qandding.domain.user.controller;

import com.qandding.domain.user.dto.UserDtos;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.service.UserService;
import com.qandding.domain.user.service.UserPostService;
import com.qandding.domain.user.dto.UserPostDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;
    private final UserPostService userPostService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    public ResponseEntity<UserDtos.Response> me(@AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        log.info("내 정보 조회 요청 - userId: {}", customPrincipal.getUserId());
        return ResponseEntity.ok(userService.getUserResponse(customPrincipal.getUserId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 부분 업데이트", description = "현재 로그인된 사용자의 닉네임, 학년, 전공을 부분 업데이트합니다.")
    public ResponseEntity<UserDtos.Response> updateProfile(
            @Valid @RequestBody UserDtos.UpdateProfileRequest req,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("프로필 업데이트 요청 - userId: {}", customPrincipal.getUserId());
        UserDtos.Response updatedUser = userService.updateProfile(customPrincipal.getUserId(), req);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/complete-profile")
    @Operation(summary = "프로필 정보 완성", description = "최초 로그인 후 사용자 프로필 정보를 완성합니다.")
    public ResponseEntity<UserDtos.Response> completeProfile(
            @Valid @RequestBody UserDtos.CompleteProfileRequest req,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("프로필 완성 요청 - userId: {}", customPrincipal.getUserId());
        UserDtos.Response updatedUser = userService.completeUserProfile(customPrincipal.getUserId(), req);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자를 탈퇴 처리합니다.")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        log.info("회원 탈퇴 요청 - userId: {}", customPrincipal.getUserId());
        userService.delete(customPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts")
    @Operation(summary = "내가 쓴 글 보기", description = "현재 로그인된 사용자가 작성한 질문글과 답변글을 페이징하여 조회합니다.")
    public ResponseEntity<UserPostDtos.UserPostsResponse> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("내가 쓴 글 조회 요청 - userId: {}, page: {}", customPrincipal.getUserId(), page);
        UserPostDtos.UserPostsResponse response = userPostService.getUserPosts(customPrincipal.getUserId(), page, 10);
        return ResponseEntity.ok(response);
    }
}