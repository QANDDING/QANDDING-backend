package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.service.AnswerSelectionService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import com.qandding.global.common.error.ApiErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
@Tag(name = "Answer Selection", description = "답변 채택/취소 API")
public class AnswerSelectionController {

    private final AnswerSelectionService answerSelectionService;

    @PostMapping("/{answerPostId}/select")
    @Operation(summary = "답변 채택", description = "질문 작성자가 특정 답변을 채택합니다. 질문당 1개만 가능하며, 기존 채택이 있으면 변경됩니다. (AI 답변은 채택할 수 없습니다)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채택 완료",
                content = @Content(mediaType = "application/json",
                        examples = @ExampleObject(name = "select-ok", value = "")
                )
        ),
        @ApiResponse(responseCode = "400", description = "AI 답변 채택 시도 또는 유효하지 않은 요청",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ApiErrorResponse.class),
                        examples = @ExampleObject(name = "ai-not-selectable", value = "{\n  \"code\": \"ANSWER_NOT_SELECTABLE\", \"message\": \"AI 답변은 채택할 수 없습니다.\", \"timestamp\": \"2025-08-19T12:34:56Z\", \"errors\": []\n}"))
        )
    })
    public ResponseEntity<Void> select(@io.swagger.v3.oas.annotations.Parameter(description = "답변 ID", example = "22") @PathVariable Long answerPostId) {
        CustomUserPrincipal principal = getPrincipal();
        answerSelectionService.select(answerPostId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/question/{questionPostId}/selection")
    @Operation(summary = "채택 취소", description = "질문 작성자가 채택을 취소합니다.")
    @ApiResponse(responseCode = "204", description = "취소 완료",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "unselect-no-content", value = "")
            )
    )
    public ResponseEntity<Void> unselect(@io.swagger.v3.oas.annotations.Parameter(description = "질문 ID", example = "101") @PathVariable Long questionPostId) {
        CustomUserPrincipal principal = getPrincipal();
        answerSelectionService.unselect(questionPostId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    private CustomUserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return (CustomUserPrincipal) authentication.getPrincipal();
    }
}
