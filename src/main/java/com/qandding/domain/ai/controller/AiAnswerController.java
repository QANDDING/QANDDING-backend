package com.qandding.domain.ai.controller;

import com.qandding.domain.ai.dto.AiAnswerDtos;
import com.qandding.domain.ai.service.AiAnswerService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/ai-answers")
@Tag(name = "AI Answer", description = "AI 답변 생성 API")
@RequiredArgsConstructor
public class AiAnswerController {

    private final AiAnswerService aiAnswerService;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 답변 생성/재생성 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AiAnswerDtos.Detail.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @Operation(summary = "AI 답변 생성/재생성", description = "프롬프트 또는 파일(OCR) 기반으로 AI 답변을 생성하거나, 기존 AI 답변이 있다면 대체합니다.")
    public ResponseEntity<AiAnswerDtos.Detail> generateUnified(
            @Parameter(description = "질문 ID") @RequestParam("questionPostId") Long questionPostId,
            @Parameter(description = "프롬프트 (선택)") @RequestParam(value = "prompt", required = false) String prompt,
            @Parameter(description = "제목 (선택)") @RequestParam(value = "title", required = false) String title,
            @Parameter(description = "파일(이미지/PDF, 선택)") @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) throws IOException {
        log.info("AI 답변 생성 요청 - userId: {}, questionPostId: {}", customPrincipal.getUserId(), questionPostId);
        // The service method now creates an AnswerPost with type AI
        AiAnswerDtos.Detail detail = aiAnswerService.generateOrReplace(questionPostId, prompt, title, file, customPrincipal.getUserId());
        log.info("AI 답변 생성 완료 - answerPostId: {}, userId: {}", detail.getId(), customPrincipal.getUserId());
        return ResponseEntity.ok(detail);
    }

    // The list and get-by-id endpoints are removed as they were based on the deleted AiAnswer entity.
    // This functionality can be re-evaluated and added to a more generic AnswerController if needed.
}
