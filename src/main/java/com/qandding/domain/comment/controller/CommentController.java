package com.qandding.domain.comment.controller;

import com.qandding.domain.comment.dto.CommentDtos;
import com.qandding.domain.comment.repository.CommentQueryRepository;
import com.qandding.domain.comment.service.CommentService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.qandding.global.common.error.ApiErrorResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Pageable imports removed; using explicit page/size params
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "   Comment", description = "댓글 관련 API")
public class CommentController {

    private final CommentService commentService;
    private final CommentQueryRepository commentQueryRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "댓글 생성", description = "텍스트와 파일(이미지/PDF)을 멀티파트로 받아 댓글을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 생성 성공 (ID 반환)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "integer", format = "int64"),
                            examples = @ExampleObject(value = "123"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"BAD_REQUEST\", \"message\": \"Bad request.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증에 실패했습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"NOT_FOUND\", \"message\": \"답변을 찾을 수 없습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Internal server error.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<Long> create(
            @Parameter(description = "답변 ID") @RequestParam("answerPostId") Long answerPostId,
            @Parameter(description = "댓글 내용") @RequestParam("content") String content,
            @Parameter(description = "첨부 파일 목록(이미지/PDF)") @RequestPart(value = "files", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> files
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal customPrincipal)) {
            return ResponseEntity.status(401).build();
        }

        log.info("Creating comment for answer: {}, user: {}", answerPostId, customPrincipal.getUserId());

        Long commentId = commentService.createCommentWithFiles(answerPostId, content, files, customPrincipal.getUserId());
        return ResponseEntity.ok(commentId);
    }

    @GetMapping
    @Operation(summary = "댓글 목록 조회", description = "특정 답변의 댓글(대댓글 포함)을 스레드 형태로 페이징 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentDtos.Thread.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"content\": [\n" +
                                            "    {\n" +
                                            "      \"parent\": {\n" +
                                            "        \"id\": 1,\n" +
                                            "        \"nickname\": \"User1\",\n" +
                                            "        \"content\": \"Parent comment content\",\n" +
                                            "        \"createdAt\": \"2025-08-19T10:00:00\",\n" +
                                            "        \"imageUrls\": [\"string\",\"string\"]\n" +
                                            "      },\n" +
                                            "      \"replies\": [\n" +
                                            "        {\n" +
                                            "          \"id\": 2,\n" +
                                            "          \"nickname\": \"User2\",\n" +
                                            "          \"content\": \"Reply comment content\",\n" +
                                            "          \"createdAt\": \"2025-08-19T10:05:00\",\n" +
                                            "          \"imageUrls\": [\"string\",\"string\"]\n" +
                                            "        }\n" +
                                            "      ]\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"pageable\": {\n" +
                                            "    \"pageNumber\": 0,\n" +
                                            "    \"pageSize\": 10,\n" +
                                            "    \"sort\": {\"empty\": false, \"sorted\": true, \"unsorted\": false},\n" +
                                            "    \"offset\": 0,\n" +
                                            "    \"paged\": true,\n" +
                                            "    \"unpaged\": false\n" +
                                            "  },\n" +
                                            "  \"last\": false,\n" +
                                            "  \"totalPages\": 1,\n" +
                                            "  \"totalElements\": 1,\n" +
                                            "  \"size\": 10,\n" +
                                            "  \"number\": 0,\n" +
                                            "  \"sort\": {\"empty\": false, \"sorted\": true, \"unsorted\": false},\n" +
                                            "  \"first\": true,\n" +
                                            "  \"numberOfElements\": 1,\n" +
                                            "  \"empty\": false\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"BAD_REQUEST\", \"message\": \"Bad request.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Internal server error.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<PageResponse<CommentDtos.Thread>> list(
            @Parameter(description = "답변 ID") @RequestParam Long answerPostId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        var threads = commentService.listThreads(answerPostId, page, size);
        return ResponseEntity.ok(PageResponse.of(threads));
    }

    @PostMapping(value = "/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "대댓글 생성", description = "특정 댓글에 대한 답글을 생성합니다. 대댓글에 대한 댓글은 허용되지 않습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "대댓글 생성 성공 (ID 반환)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "integer", format = "int64"),
                            examples = @ExampleObject(value = "456"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"BAD_REQUEST\", \"message\": \"Bad request.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증에 실패했습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "404", description = "부모 댓글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"NOT_FOUND\", \"message\": \"부모 댓글을 찾을 수 없습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Internal server error.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<Long> reply(
            @Parameter(description = "부모 댓글 ID") @RequestParam("parentCommentId") Long parentCommentId,
            @Parameter(description = "답글 내용") @RequestParam("content") String content,
            @Parameter(description = "첨부 파일 목록(이미지/PDF)") @RequestPart(value = "files", required = false) java.util.List<org.springframework.web.multipart.MultipartFile> files
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal customPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        Long id = commentService.createReplyWithFiles(parentCommentId, content, files, customPrincipal.getUserId());
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED\", \"message\": \"인증에 실패했습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"FORBIDDEN\", \"message\": \"권한이 없습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"NOT_FOUND\", \"message\": \"댓글을 찾을 수 없습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Internal server error.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<Void> delete(@Parameter(description = "댓글 ID") @PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal customPrincipal)) {
            return ResponseEntity.status(401).build();
        }

        log.info("Attempting to delete comment with id: {}, user: {}", id, customPrincipal.getUserId());

        commentService.deleteComment(id, customPrincipal.getUserId());

        return ResponseEntity.noContent().build();
    }

}
