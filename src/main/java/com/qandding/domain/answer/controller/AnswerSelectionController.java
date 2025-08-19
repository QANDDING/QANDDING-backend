package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.service.AnswerSelectionService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.springframework.http.MediaType;
import com.qandding.global.common.error.ApiErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/answers/selection")
@RequiredArgsConstructor
@Tag(name = "Answer Selection", description = "질문자가 답변 채택/취소하는 API")
public class AnswerSelectionController {

    private final AnswerSelectionService answerSelectionService;

    @PostMapping
    @Operation(summary = "답변 채택", description = "질문 작성자가 유저 답변을 채택합니다. AI 답변은 채택 불가.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채택 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "integer", format = "int64"),
                            examples = @ExampleObject(value = "321"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증이 필요합니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"FORBIDDEN_ACTION\", \"message\": \"권한이 없습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"ANSWER_NOT_FOUND\", \"message\": \"답변을 찾을 수 없습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (AI 답변 채택 시 등)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"BAD_REQUEST\", \"message\": \"잘못된 요청입니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<Long> adopt(@Parameter(description = "채택할 답변 ID", required = true) @RequestParam("answerPostId") Long answerPostId,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        // JWT 토큰 검증 (Spring Security가 자동으로 처리)
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 답변 채택 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.info("답변 채택 요청 - answerPostId: {}, userId: {}", answerPostId, customPrincipal.getUserId());
        
        try {
            Long selectedId = answerSelectionService.adopt(answerPostId, customPrincipal.getUserId());
            log.info("답변 채택 완료 - answerPostId: {}, selectedId: {}, userId: {}", answerPostId, selectedId, customPrincipal.getUserId());
            return ResponseEntity.ok(selectedId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("답변 채택 중 오류 발생 - answerPostId: {}, userId: {}", answerPostId, customPrincipal.getUserId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "답변 채택 중 오류가 발생했습니다.");
        }
    }

    @DeleteMapping
    @Operation(summary = "채택 취소", description = "질문 작성자가 현재 채택된 답변을 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증이 필요합니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"FORBIDDEN_ACTION\", \"message\": \"권한이 없습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<Void> unadopt(@Parameter(description = "질문 ID", required = true) @RequestParam("questionPostId") Long questionPostId,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        // JWT 토큰 검증 (Spring Security가 자동으로 처리)
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 답변 채택 취소 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.info("답변 채택 취소 요청 - questionPostId: {}, userId: {}", questionPostId, customPrincipal.getUserId());
        
        try {
            answerSelectionService.unadopt(questionPostId, customPrincipal.getUserId());
            log.info("답변 채택 취소 완료 - questionPostId: {}, userId: {}", questionPostId, customPrincipal.getUserId());
            return ResponseEntity.noContent().build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("답변 채택 취소 중 오류 발생 - questionPostId: {}, userId: {}", questionPostId, customPrincipal.getUserId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "답변 채택 취소 중 오류가 발생했습니다.");
        }
    }
}
