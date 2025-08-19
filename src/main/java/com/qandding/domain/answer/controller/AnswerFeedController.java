package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.service.AnswerFeedService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/answers")
@Tag(name = "Answer Feed", description = "AI 최상위 + 사용자 답변 페이징 API")
@RequiredArgsConstructor
public class AnswerFeedController {

    private final AnswerFeedService answerFeedService;

    @GetMapping("/combined")
    @Operation(summary = "AI 최상단 + 사용자 답변 페이징", description = "AI 답변을 최상단으로, 이후 사용자 답변을 3개씩 페이징하여 상세 정보(제목/내용/시간/이미지)를 반환합니다. 유저 답변에는 채택 여부(isAdopted)가 포함되며 AI 답변은 항상 false입니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnswerDtos.Combined.class),
                            examples = @ExampleObject(
                                    name = "combined-example",
                                    value = "{\n" +
                                            "  \"ai\": {\n" +
                                            "    \"id\": 100,\n" +
                                            "    \"title\": \"AI 답변 제목\",\n" +
                                            "    \"content\": \"AI가 생성한 답변 내용\",\n" +
                                            "    \"authorNickname\": \"Qandding-AI\",\n" +
                                            "    \"createdAt\": \"2025-08-19T10:00:00\",\n" +
                                            "    \"imageUrls\": [\"https://example.com/ai1.png\"],\n" +
                                            "    \"ai\": true,\n" +
                                            "    \"isAdopted\": false\n" +
                                            "  },\n" +
                                            "  \"users\": {\n" +
                                            "    \"content\": [\n" +
                                            "      {\n" +
                                            "        \"id\": 201,\n" +
                                            "        \"title\": \"사용자 답변 1 제목\",\n" +
                                            "        \"content\": \"사용자 답변 1 내용\",\n" +
                                            "        \"authorNickname\": \"user1\",\n" +
                                            "        \"createdAt\": \"2025-08-19T10:05:00\",\n" +
                                            "        \"imageUrls\": [\"https://example.com/u1-1.png\", \"https://example.com/u1-2.png\"],\n" +
                                            "        \"ai\": false,\n" +
                                            "        \"isAdopted\": true\n" +
                                            "      },\n" +
                                            "      {\n" +
                                            "        \"id\": 202,\n" +
                                            "        \"title\": \"사용자 답변 2 제목\",\n" +
                                            "        \"content\": \"사용자 답변 2 내용\",\n" +
                                            "        \"authorNickname\": \"user2\",\n" +
                                            "        \"createdAt\": \"2025-08-19T10:10:00\",\n" +
                                            "        \"imageUrls\": [],\n" +
                                            "        \"ai\": false,\n" +
                                            "        \"isAdopted\": false\n" +
                                            "      }\n" +
                                            "    ],\n" +
                                            "    \"page\": 0,\n" +
                                            "    \"size\": 3,\n" +
                                            "    \"totalElements\": 10,\n" +
                                            "    \"totalPages\": 4\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<AnswerDtos.Combined> combined(
            @Parameter(description = "질문 ID", required = true, schema = @Schema(type = "integer", format = "int64")) @RequestParam("questionPostId") Long questionPostId,
            @Parameter(description = "페이지 번호", schema = @Schema(type = "integer", defaultValue = "0")) @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", schema = @Schema(type = "integer", defaultValue = "3")) @RequestParam(value = "size", defaultValue = "3") int size,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        // JWT 토큰 검증 (Spring Security가 자동으로 처리)
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 답변 피드 조회 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.info("답변 피드 조회 요청 - userId: {}, questionPostId: {}, page: {}", 
                customPrincipal.getUserId(), questionPostId, page);
        
        try {
            AnswerDtos.Combined combinedFeed = answerFeedService.getCombinedFeed(questionPostId, page, size);
            log.info("답변 피드 조회 완료 - userId: {}, questionPostId: {}", 
                    customPrincipal.getUserId(), questionPostId);
            return ResponseEntity.ok(combinedFeed);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("답변 피드 조회 중 오류 발생 - userId: {}, questionPostId: {}", 
                    customPrincipal.getUserId(), questionPostId, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "답변 피드 조회 중 오류가 발생했습니다.");
        }
    }
}

