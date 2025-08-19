package com.qandding.domain.ai.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.qandding.domain.ai.dto.AiAnswerDtos;
import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.ai.repository.AiAnswerRepository;
import com.qandding.domain.ai.service.AiAnswerService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ai-answers")
@Tag(name = "AI 답변", description = "AI 답변 생성 및 관리 API")
@RequiredArgsConstructor
public class AiAnswerController {

    private final AiAnswerService aiAnswerService;
    private final AiAnswerRepository aiAnswerRepository;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI 답변 생성/재생성", description = "프롬프트 또는 파일(OCR) 기반으로 AI 답변을 생성하거나 재생성(대체)합니다.")
    public ResponseEntity<AiAnswerDtos.Detail> generateUnified(
            @Parameter(description = "질문 ID") @RequestParam("questionPostId") Long questionPostId,
            @Parameter(description = "프롬프트 (선택)") @RequestParam(value = "prompt", required = false) String prompt,
            @Parameter(description = "제목 (선택)") @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "파일(이미지/PDF, 선택)") @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) throws IOException {
        // JWT 토큰 검증 (Spring Security가 자동으로 처리)
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 AI 답변 생성 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.info("AI 답변 생성 요청 - userId: {}, questionPostId: {}", customPrincipal.getUserId(), questionPostId);
        
        try {
            AiAnswerDtos.Detail detail = aiAnswerService.generateOrReplace(questionPostId, prompt, title, file, customPrincipal.getUserId());
            log.info("AI 답변 생성 완료 - userId: {}, questionPostId: {}", customPrincipal.getUserId(), questionPostId);
            return ResponseEntity.ok(detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 답변 생성 중 오류 발생 - userId: {}, questionPostId: {}", customPrincipal.getUserId(), questionPostId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 답변 생성 중 오류가 발생했습니다.");
        }
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
}
