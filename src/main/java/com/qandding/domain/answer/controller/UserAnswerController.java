package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.UserAnswerDtos;
import com.qandding.domain.answer.repository.UserAnswerQueryRepository;
import com.qandding.domain.answer.service.UserAnswerService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import com.qandding.global.common.response.CommonResponse;
import com.qandding.global.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user-answers")
@RequiredArgsConstructor
@Tag(name = "User Answer", description = "사용자 답변 관련 API")
public class UserAnswerController {

    private final UserAnswerService userAnswerService;
    private final UserAnswerQueryRepository userAnswerQueryRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사용자 답변 생성", description = "텍스트와 파일(이미지/PDF)을 멀티파트로 받아 사용자 답변을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 답변 생성 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> create(
            @Parameter(description = "질문 ID") @RequestParam("questionPostId") Long questionPostId,
            @Parameter(description = "답변 제목") @RequestParam("title") String title,
            @Parameter(description = "답변 내용") @RequestParam("content") String content,
            @Parameter(description = "첨부 파일 목록(이미지/PDF)") @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.debug("create() 호출됨 - questionPostId: {}, title: {}, userId: {}", questionPostId, title, customPrincipal.getUserId());
        log.info("사용자 답변 생성 요청 - userId: {}, questionPostId: {}, title: {}",
                customPrincipal.getUserId(), questionPostId, title);

        Long userAnswerId = userAnswerService.createUserAnswerWithFiles(questionPostId, title, content, files, customPrincipal.getUserId());

        log.info("사용자 답변 생성 완료 - answerId: {}, userId: {}", userAnswerId, customPrincipal.getUserId());
        log.debug("create() 반환 - userAnswerId: {}", userAnswerId);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.CREATED, userAnswerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 답변 상세 조회", description = "특정 사용자 답변의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    public ResponseEntity<UserAnswerDtos.Detail> get(@Parameter(description = "답변 ID") @PathVariable Long id) {
        log.debug("get() 호출됨 - id: {}", id);
        // 이 API는 인증이 필요 없도록 설계 (누구나 답변 상세 내용을 볼 수 있음)
        log.info("Fetching user answer with id: {}", id);
        UserAnswerDtos.Detail detail = userAnswerService.getUserAnswerDetail(id);
        log.debug("get() 반환 - userAnswerId: {}", detail.getId());
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 답변 삭제", description = "특정 사용자 답변을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 답변 삭제 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> delete(
            @Parameter(description = "답변 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.debug("delete() 호출됨 - id: {}, userId: {}", id, customPrincipal.getUserId());
        log.info("사용자 답변 삭제 요청 - answerId: {}, userId: {}", id, customPrincipal.getUserId());
        userAnswerService.deleteUserAnswer(id, customPrincipal.getUserId());
        log.info("사용자 답변 삭제 완료 - answerId: {}, userId: {}", id, customPrincipal.getUserId());
        log.debug("delete() 반환 - void");
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.NO_CONTENT, id));
    }
}
