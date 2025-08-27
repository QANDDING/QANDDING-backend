package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.service.AnswerFeedService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "AI 최상단 + 사용자 답변 페이징", description = "AI 답변을 최상단으로, 이후 사용자 답변을 3개씩 페이징하여 상세 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnswerDtos.Combined.class),
                            examples = @ExampleObject(value = """
{
  "ai": {
    "id": 1,
    "title": "AI 답변 제목",
    "content": "AI 답변 내용입니다.",
    "authorNickname": "AI",
    "createdAt": "2025-08-20T10:00:00Z",
    "imageUrls": [],
    "ai": true,
    "isAdopted": false
  },
  "users": {
    "content": [
      {
        "id": 101,
        "title": "사용자 답변 1 제목",
        "content": "사용자 답변 1 내용입니다.",
        "authorNickname": "user1",
        "createdAt": "2025-08-20T10:05:00Z",
        "imageUrls": ["http://example.com/image1.jpg"],
        "isAdopted": false
      },
      {
        "id": 102,
        "title": "사용자 답변 2 제목",
        "content": "사용자 답변 2 내용입니다.",
        "authorNickname": "user2",
        "createdAt": "2025-08-20T10:10:00Z",
        "imageUrls": [],
        "isAdopted": true
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 3,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 2,
    "last": true,
    "first": true,
    "numberOfElements": 2,
    "size": 3,
    "number": 0,
    "empty": false
  }
}
"""
                            )
                    )),
                        @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<AnswerDtos.Combined> combined(
            @Parameter(description = "질문 ID", required = true) @RequestParam("questionPostId") Long questionPostId,
            @Parameter(description = "페이지 번호") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(value = "size", defaultValue = "3") int size,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        // 인증 확인
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 답변 피드 조회 요청");
            return ResponseEntity.status(401).build();
        }
        
        log.info("답변 피드 조회 요청 - userId: {}, questionPostId: {}, page: {}",
                customPrincipal.getUserId(), questionPostId, page);

        AnswerDtos.Combined combinedFeed = answerFeedService.getCombinedFeed(questionPostId, page, size);
        
        log.info("답변 피드 조회 완료 - userId: {}, questionPostId: {}",
                customPrincipal.getUserId(), questionPostId);
        
        return ResponseEntity.ok(combinedFeed);
    }
}