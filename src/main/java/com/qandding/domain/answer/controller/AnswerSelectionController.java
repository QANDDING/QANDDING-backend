package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.service.AnswerSelectionService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
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
        @ApiResponse(responseCode = "200", description = "답변 채택 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> adopt(
            @Parameter(description = "채택할 답변 ID", required = true) @RequestParam("answerPostId") Long answerPostId,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        
        log.info("답변 채택 요청 - answerPostId: {}, userId: {}", answerPostId, customPrincipal.getUserId());
        Long selectedId = answerSelectionService.adopt(answerPostId, customPrincipal.getUserId());
        log.info("답변 채택 완료 - answerPostId: {}, selectedId: {}, userId: {}", answerPostId, selectedId, customPrincipal.getUserId());
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, selectedId));
    }

    @DeleteMapping
    @Operation(summary = "채택 취소", description = "질문 작성자가 현재 채택된 답변을 취소합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채택 취소 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> unadopt(
            @Parameter(description = "질문 ID", required = true) @RequestParam("questionPostId") Long questionPostId,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        
        log.info("답변 채택 취소 요청 - questionPostId: {}, userId: {}", questionPostId, customPrincipal.getUserId());
        answerSelectionService.unadopt(questionPostId, customPrincipal.getUserId());
        log.info("답변 채택 취소 완료 - questionPostId: {}, userId: {}", questionPostId, customPrincipal.getUserId());
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.NO_CONTENT, questionPostId));
    }
}