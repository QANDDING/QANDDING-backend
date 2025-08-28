package com.qandding.domain.comment.controller;

import com.qandding.domain.comment.dto.CommentCreateRequest;
import com.qandding.domain.comment.dto.CommentDtos;
import com.qandding.domain.comment.dto.ReplyCreateRequest;
import com.qandding.domain.comment.service.CommentService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import com.qandding.global.common.response.CommonResponse;
import com.qandding.global.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "댓글 생성 (Presigned URL 방식)", description = "S3에 선업로드 후, 파일 URL과 함께 댓글 내용을 JSON으로 받아 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "댓글 생성 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "code": "CREATED",
                      "message": "생성 성공",
                      "data": 1
                    }
                    """))),
                @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> create(
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("댓글 생성 요청 - userId: {}, answerPostId: {}", customPrincipal.getUserId(), request.getAnswerPostId());
        Long commentId = commentService.createCommentWithImageUrls(
            request.getAnswerPostId(),
            request.getContent(),
            request.getImageUrls(),
            customPrincipal.getUserId()
        );
        log.info("댓글 생성 완료 - commentId: {}, userId: {}", commentId, customPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(ResponseCode.CREATED, commentId));
    }

        @GetMapping
    @Operation(
        summary = "댓글 목록 조회",
        description = "특정 답변의 댓글(대댓글 포함)을 스레드 형태로 페이징 조회합니다.",
        security = {}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "content": [
                        {
                          "parent": {
                            "id": 1,
                            "nickname": "parent_user",
                            "content": "이것은 부모 댓글입니다.",
                            "createdAt": "2025-08-21T10:00:00Z",
                            "imageUrls": [],
                            "replyCount": 1,
                            "totalCommentCount": 2
                          },
                          "replies": [
                            {
                              "id": 2,
                              "nickname": "reply_user",
                              "content": "이것은 대댓글입니다.",
                              "createdAt": "2025-08-21T10:05:00Z",
                              "imageUrls": [],
                              "replyCount": 0,
                              "totalCommentCount": 0
                            }
                          ]
                        }
                      ],
                      "page": 0,
                      "size": 10,
                      "totalElements": 1,
                      "totalPages": 1,
                      "last": true,
                      "totalCommentsOverall": 2
                    }
                    """)))
    })
    public ResponseEntity<PageResponse<CommentDtos.Thread>> list(
            @Parameter(description = "답변 ID") @RequestParam Long answerPostId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        log.debug("list() 호출됨 - answerPostId: {}, page: {}, size: {}", answerPostId, page, size);
        var threads = commentService.listThreads(answerPostId, page, size);
        log.debug("list() 반환 - PageResponse: {}개 항목", threads.getTotalElements());
        return ResponseEntity.ok(PageResponse.of(threads));
    }

    @PostMapping(value = "/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "대댓글 생성 (Presigned URL 방식)", description = "S3에 선업로드 후, 파일 URL과 함께 대댓글 내용을 JSON으로 받아 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "대댓글 생성 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "code": "CREATED",
                      "message": "생성 성공",
                      "data": 2
                    }
                    """))),
                @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "부모 댓글을 찾을 수 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> reply(
            @RequestBody @Valid ReplyCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("대댓글 생성 요청 - userId: {}, parentCommentId: {}", customPrincipal.getUserId(), request.getParentCommentId());
        Long id = commentService.createReplyWithImageUrls(
            request.getParentCommentId(),
            request.getContent(),
            request.getImageUrls(),
            customPrincipal.getUserId()
        );
        log.info("대댓글 생성 완료 - commentId: {}, userId: {}", id, customPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(ResponseCode.CREATED, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "댓글 삭제 성공",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CommonResponse.class),
                        examples = @ExampleObject(value = """
                            {
                              "code": "NO_CONTENT",
                              "message": "성공",
                              "data": 1
                            }
                            """))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> delete(
            @Parameter(description = "댓글 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        log.debug("delete() 호출됨 - id: {}, userId: {}", id, customPrincipal.getUserId());
        log.info("Attempting to delete comment with id: {}, user: {}", id, customPrincipal.getUserId());
        commentService.deleteComment(id, customPrincipal.getUserId());
        log.info("댓글 삭제 완료 - commentId: {}, userId: {}", id, customPrincipal.getUserId());
        log.debug("delete() 반환 - void");
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.NO_CONTENT, id));
    }
}
