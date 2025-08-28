package com.qandding.domain.user.controller;

import com.qandding.domain.user.dto.UserDtos;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.domain.user.service.UserService;
import com.qandding.domain.user.service.UserPostService;
import com.qandding.domain.user.dto.UserPostDtos;
import com.qandding.global.common.response.CommonResponse;
import com.qandding.global.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> withdraw(@AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        log.info("회원 탈퇴 요청 - userId: {}", customPrincipal.getUserId());
        userService.delete(customPrincipal.getUserId());
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.NO_CONTENT, customPrincipal.getUserId()));
    }

    @GetMapping("/posts")
    @Operation(summary = "내가 쓴 글 보기", description = "현재 로그인된 사용자가 작성한 질문글과 답변글을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 내가 쓴 글 목록을 조회했습니다.",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserPostDtos.UserPostsResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "내가 쓴 글 목록 예시",
                                    summary = "내가 쓴 글 목록 조회 성공 예시",
                                    value = """
{
  "posts": [
    {
      "postType": "QUESTION",
      "postId": 101,
      "title": "첫 번째 질문입니다.",
      "createdAt": "2025-08-19T10:00:00Z",
      "originalQuestionId": null
    },
    {
      "postType": "ANSWER",
      "postId": 205,
      "title": "답변 드립니다.",
      "createdAt": "2025-08-18T15:30:00Z",
      "originalQuestionId": 123
    },
    {
      "postType": "QUESTION",
      "postId": 102,
      "title": "두 번째 질문입니다.",
      "createdAt": "2025-08-17T09:00:00Z",
      "originalQuestionId": null
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 48,
  "totalPages": 5
}
                                    """
                            )
                    )),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자입니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다 (예: 유효하지 않은 페이지 번호)."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.")
    })
    public ResponseEntity<UserPostDtos.UserPostsResponse> getMyPosts(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "검색 키워드 (제목 또는 내용)", required = false) @RequestParam(required = false) String keyword,
            @Parameter(description = "글 종류 필터링 (QUESTION 또는 ANSWER)", required = false) @RequestParam(required = false) String postType,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("내가 쓴 글 조회 요청 - userId: {}, page: {}, keyword: {}, postType: {}", customPrincipal.getUserId(), page, keyword, postType);
        UserPostDtos.UserPostsResponse response = userPostService.getUserPosts(customPrincipal.getUserId(), page, 10, keyword, postType);
        return ResponseEntity.ok(response);
    }
}