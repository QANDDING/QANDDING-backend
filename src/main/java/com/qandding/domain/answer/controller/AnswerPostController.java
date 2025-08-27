package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.AnswerCreateRequest;
import com.qandding.domain.answer.dto.AnswerPostDtos;
import com.qandding.domain.answer.service.AnswerPostService;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
@Tag(name = "Answer Post", description = "답변 게시글 관련 API (생성/조회/삭제)")
public class AnswerPostController {

    private final AnswerPostService answerPostService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "답변 게시글 생성 (Presigned URL 방식)", description = "S3에 선업로드 후, 파일 URL과 함께 답변 내용을 JSON으로 받아 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "답변 게시글 생성 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                    {
                      "code": "CREATED",
                      "message": "생성 성공",
                      "data": 1
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> create(
            @RequestBody @Valid AnswerCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("답변 게시글 생성 요청 - userId: {}, questionPostId: {}, title: {}",
                customPrincipal.getUserId(), request.getQuestionPostId(), request.getTitle());

        Long answerPostId = answerPostService.createAnswerPostForUserWithImageUrls(
            request.getQuestionPostId(),
            request.getTitle(),
            request.getContent(),
            request.getImageUrls(),
            customPrincipal.getUserId()
        );

        log.info("답변 게시글 생성 완료 - answerPostId: {}, userId: {}", answerPostId, customPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(ResponseCode.CREATED, answerPostId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "답변 게시글 상세 조회", description = "특정 답변 게시글의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AnswerPostDtos.Detail.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                    {
                      "id": 1,
                      "title": "미적분학 1번 문제 답변입니다.",
                      "content": "이 문제의 풀이 과정은 다음과 같습니다...",
                      "authorNickname": "답변자1",
                      "createdAt": "2025-08-27T10:00:00",
                      "imageUrls": ["https://example.com/answer_image1.jpg"]
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<AnswerPostDtos.Detail> get(@Parameter(description = "답변 ID (AnswerPost ID)") @PathVariable Long id) {
        log.info("Fetching answer post with id: {}", id);
        AnswerPostDtos.Detail detail = answerPostService.getAnswerPostDetail(id);
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "답변 게시글 삭제", description = "특정 답변 게시글을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "답변 게시글 삭제 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CommonResponse.class),
                        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                            {
                              "code": "NO_CONTENT",
                              "message": "성공",
                              "data": 1
                            }
                            """))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> delete(
            @Parameter(description = "답변 ID (AnswerPost ID)") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("답변 게시글 삭제 요청 - answerPostId: {}, userId: {}", id, customPrincipal.getUserId());
        answerPostService.deleteAnswerPost(id, customPrincipal.getUserId());
        log.info("답변 게시글 삭제 완료 - answerPostId: {}, userId: {}", id, customPrincipal.getUserId());
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.NO_CONTENT, id));
    }
}