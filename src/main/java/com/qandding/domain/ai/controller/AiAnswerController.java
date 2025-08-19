package com.qandding.domain.ai.controller;

import com.qandding.domain.ai.dto.AiAnswerDtos;
import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.ai.repository.AiAnswerRepository;
import com.qandding.domain.ai.service.AiAnswerService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ai-answers")
@Tag(name = "AI 답변", description = "AI 답변 생성 및 관리 API")
@RequiredArgsConstructor
public class AiAnswerController {

    private final AiAnswerService aiAnswerService;
    private final AiAnswerRepository aiAnswerRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 통합 엔드포인트 사용으로 별도 요청 레코드 제거

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI 답변 생성/재생성", description = "프롬프트 또는 파일(OCR) 기반으로 AI 답변을 생성하거나 재생성(대체)합니다.")
    public ResponseEntity<AiAnswerDtos.Detail> generateUnified(
            @Parameter(description = "질문 ID") @RequestParam("questionPostId") Long questionPostId,
            @Parameter(description = "프롬프트 (선택)") @RequestParam(value = "prompt", required = false) String prompt,
            @Parameter(description = "제목 (선택)") @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "파일(이미지/PDF, 선택)") @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        CustomUserPrincipal customPrincipal = getCustomUserPrincipal();
        AiAnswerDtos.Detail detail = aiAnswerService.generateOrReplace(questionPostId, prompt, title, file, customPrincipal.getUserId());
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    @Operation(summary = "AI 답변 목록 조회", description = "특정 질문에 대한 AI 답변 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PageResponse<AiAnswerDtos.Summary>> list(
            @Parameter(description = "질문 ID") @RequestParam Long questionPostId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        Pageable validatedPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AiAnswer> aiAnswerPage = aiAnswerRepository.findByQuestionPostId(questionPostId, validatedPageable);
        List<AiAnswerDtos.Summary> content = aiAnswerPage.getContent().stream()
                .map(AiAnswerDtos.Summary::from)
                .toList();
        Page<AiAnswerDtos.Summary> summaryPage = new PageImpl<>(content, validatedPageable, aiAnswerPage.getTotalElements());
        return ResponseEntity.ok(PageResponse.of(summaryPage));
    }

    // Pageable-based helper removed; using explicit params

    @GetMapping("/{id}")
    @Operation(summary = "AI 답변 상세 조회", description = "특정 AI 답변의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    public ResponseEntity<AiAnswerDtos.Detail> get(@Parameter(description = "AI 답변 ID") @PathVariable Long id) {
        AiAnswerDtos.Detail detail = aiAnswerService.getAiAnswerDetail(id);
        return ResponseEntity.ok(detail);
    }

    private CustomUserPrincipal getCustomUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return (CustomUserPrincipal) authentication.getPrincipal();
    }
}
