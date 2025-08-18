package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.service.AnswerFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<AnswerDtos.Combined> combined(
            @RequestParam("questionPostId") Long questionPostId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "3") int size
    ) {
        AnswerDtos.Combined combinedFeed = answerFeedService.getCombinedFeed(questionPostId, page, size);
        return ResponseEntity.ok(combinedFeed);
    }
}

