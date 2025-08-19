package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.service.AnswerFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "AI 최상단 + 사용자 답변 페이징", description = "AI 답변을 최상단으로, 이후 사용자 답변을 3개씩 페이징하여 상세 정보(제목/내용/시간/이미지)를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnswerDtos.Combined.class),
                            examples = @ExampleObject(
                                    name = "combined-example",
                                    value = "{\n  \"ai\": {\n    \"id\": 11, \"title\": \"AI 답변 - 적분 문제\", \"content\": \"...\",\n    \"authorNickname\": \"AI\", \"createdAt\": \"2025-08-19T12:34:56\"\n  },\n  \"users\": {\n    \"content\": [\n      {\n        \"id\": 22, \"title\": \"제 풀이\", \"content\": \"...\",\n        \"authorNickname\": \"hong\", \"createdAt\": \"2025-08-19T12:40:00\",\n        \"imageUrls\": [\"urs1\", \"urls2\"], \"accept\": true\n      },\n      {\n        \"id\": 23, \"title\": \"다른 풀이\", \"content\": \"...\",\n        \"authorNickname\": \"lee\", \"createdAt\": \"2025-08-19T12:45:00\",\n        \"imageUrls\": [\"urs3\", \"urls4\"], \"accept\": false\n      }\n    ],\n    \"page\": 0, \"size\": 3, \"totalElements\": 2, \"totalPages\": 1\n  }\n}"
                            )
                    )
            )
    })
    public ResponseEntity<AnswerDtos.Combined> combined(
            @io.swagger.v3.oas.annotations.Parameter(description = "질문 ID", example = "101") @RequestParam("questionPostId") Long questionPostId,
            @io.swagger.v3.oas.annotations.Parameter(description = "페이지 번호", example = "0") @RequestParam(value = "page", defaultValue = "0") int page,
            @io.swagger.v3.oas.annotations.Parameter(description = "페이지 크기", example = "3") @RequestParam(value = "size", defaultValue = "3") int size
    ) {
        AnswerDtos.Combined combinedFeed = answerFeedService.getCombinedFeed(questionPostId, page, size);
        return ResponseEntity.ok(combinedFeed);
    }
}
