package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.UserAnswerDtos;
import com.qandding.domain.answer.repository.UserAnswerQueryRepository;
import com.qandding.domain.answer.service.UserAnswerService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import com.qandding.global.storage.S3PresignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
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
    private final S3PresignService s3PresignService; // get method needs this

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사용자 답변 생성", description = "텍스트와 파일(이미지/PDF)을 멀티파트로 받아 사용자 답변을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    public ResponseEntity<Long> create(
            @Parameter(description = "질문 ID") @RequestParam("questionPostId") Long questionPostId,
            @Parameter(description = "답변 제목") @RequestParam("title") String title,
            @Parameter(description = "답변 내용") @RequestParam("content") String content,
            @Parameter(description = "첨부 파일 목록(이미지/PDF)") @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        CustomUserPrincipal customPrincipal = getCustomUserPrincipal();
        Long userAnswerId = userAnswerService.createUserAnswerWithFiles(questionPostId, title, content, files, customPrincipal.getUserId());
        return ResponseEntity.ok(userAnswerId);
    }

    @GetMapping
    @Operation(summary = "사용자 답변 목록 조회", description = "특정 질문에 대한 사용자 답변 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PageResponse<UserAnswerDtos.Summary>> list(
            @Parameter(description = "질문 ID") @RequestParam Long questionPostId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var pageRes = userAnswerQueryRepository.findSummaries(questionPostId, pageable);
        return ResponseEntity.ok(PageResponse.of(pageRes));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "특정 사용자의 답변 목록 조회", description = "특정 사용자가 작성한 모든 답변 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PageResponse<UserAnswerDtos.Summary>> listByUser(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var pageRes = userAnswerQueryRepository.findSummariesByUserId(userId, pageable);
        return ResponseEntity.ok(PageResponse.of(pageRes));
    }

    @GetMapping("/user/{userId}/question/{questionId}")
    @Operation(summary = "특정 사용자의 특정 질문에 대한 답변 목록 조회", description = "특정 사용자가 특정 질문에 대해 작성한 답변 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PageResponse<UserAnswerDtos.Summary>> listByUserAndQuestion(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Parameter(description = "질문 ID") @PathVariable Long questionId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var pageRes = userAnswerQueryRepository.findSummariesByUserIdAndQuestionId(userId, questionId, pageable);
        return ResponseEntity.ok(PageResponse.of(pageRes));
    }


    @GetMapping("/{id}")
    @Operation(summary = "사용자 답변 상세 조회", description = "특정 사용자 답변의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    public ResponseEntity<UserAnswerDtos.Detail> get(@Parameter(description = "답변 ID") @PathVariable Long id) {
        log.info("Fetching user answer with id: {}", id);
        UserAnswerDtos.Detail detail = userAnswerService.getUserAnswerDetail(id);
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 답변 삭제", description = "특정 사용자 답변을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "답변 ID") @PathVariable Long id) {
        CustomUserPrincipal customPrincipal = getCustomUserPrincipal();
        userAnswerService.deleteUserAnswer(id, customPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }

    private CustomUserPrincipal getCustomUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            throw new com.qandding.global.common.error.BusinessException(com.qandding.global.common.error.ErrorCode.UNAUTHORIZED);
        }
        return (CustomUserPrincipal) authentication.getPrincipal();
    }
}
