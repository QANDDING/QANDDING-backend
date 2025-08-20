package com.qandding.domain.comment.controller;

import com.qandding.domain.comment.dto.CommentDtos;
import com.qandding.domain.comment.service.CommentService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "댓글 생성", description = "텍스트와 파일(이미지/PDF)을 멀티파트로 받아 댓글을 생성합니다.")
    // ... @ApiResponses ...
    public ResponseEntity<Long> create(
            @Parameter(description = "답변 ID") @RequestParam("answerPostId") Long answerPostId,
            @Parameter(description = "댓글 내용") @RequestParam("content") String content,
            @Parameter(description = "첨부 파일 목록") @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("Creating comment for answer: {}, user: {}", answerPostId, customPrincipal.getUserId());
        Long commentId = commentService.createCommentWithFiles(answerPostId, content, files, customPrincipal.getUserId());
        log.info("댓글 생성 완료 - commentId: {}, userId: {}", commentId, customPrincipal.getUserId());
        return ResponseEntity.ok(commentId);
    }

        @GetMapping
    @Operation(summary = "댓글 목록 조회", description = "특정 답변의 댓글(대댓글 포함)을 스레드 형태로 페이징 조회합니다.")
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
                            "imageUrls": []
                          },
                          "replies": [
                            {
                              "id": 2,
                              "nickname": "reply_user",
                              "content": "이것은 대댓글입니다.",
                              "createdAt": "2025-08-21T10:05:00Z",
                              "imageUrls": []
                            }
                          ]
                        }
                      ],
                      "page": 0,
                      "size": 10,
                      "totalElements": 1,
                      "totalPages": 1,
                      "last": true
                    }
                    """)))
    })
    public ResponseEntity<PageResponse<CommentDtos.Thread>> list(
            @Parameter(description = "답변 ID") @RequestParam Long answerPostId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        var threads = commentService.listThreads(answerPostId, page, size);
        return ResponseEntity.ok(PageResponse.of(threads));
    }

    @PostMapping(value = "/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "대댓글 생성", description = "특정 댓글에 대한 답글을 생성합니다.")
    // ... @ApiResponses ...
    public ResponseEntity<Long> reply(
            @Parameter(description = "부모 댓글 ID") @RequestParam("parentCommentId") Long parentCommentId,
            @Parameter(description = "답글 내용") @RequestParam("content") String content,
            @Parameter(description = "첨부 파일 목록") @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("Creating reply for comment: {}, user: {}", parentCommentId, customPrincipal.getUserId());
        Long id = commentService.createReplyWithFiles(parentCommentId, content, files, customPrincipal.getUserId());
        log.info("대댓글 생성 완료 - commentId: {}, userId: {}", id, customPrincipal.getUserId());
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다.")
    // ... @ApiResponses ...
    public ResponseEntity<Void> delete(
            @Parameter(description = "댓글 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        log.info("Attempting to delete comment with id: {}, user: {}", id, customPrincipal.getUserId());
        commentService.deleteComment(id, customPrincipal.getUserId());
        log.info("댓글 삭제 완료 - commentId: {}, userId: {}", id, customPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }
}