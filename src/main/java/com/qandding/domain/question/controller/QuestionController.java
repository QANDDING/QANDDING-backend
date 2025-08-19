package com.qandding.domain.question.controller;

import com.qandding.domain.question.dto.QuestionDtos;
import com.qandding.domain.question.repository.QuestionQueryRepository;
import com.qandding.domain.question.service.QuestionService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.qandding.global.common.error.ApiErrorResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Tag(name = "Question", description = "질문 관련 API")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionQueryRepository questionQueryRepository;

    // JSON DTO는 사용하지 않고, 멀티파트 텍스트 파라미터로 받습니다.

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "질문 생성", description = "멀티파트로 텍스트 + 파일(선택)을 받아 질문을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질문 생성 성공 (ID 반환)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "관련 정보(사용자, 과목, 교수)를 찾을 수 없음")
    })
    public ResponseEntity<Long> create(
            @Parameter(description = "질문 제목", required = true, schema = @Schema(type = "string")) @RequestParam("title") String title,
            @Parameter(description = "질문 내용", required = true, schema = @Schema(type = "string")) @RequestParam("content") String content,
            @Parameter(description = "과목 ID", required = true, schema = @Schema(type = "integer", format = "int64")) @RequestParam("subjectId") Long subjectId,
            @Parameter(description = "교수 ID", required = true, schema = @Schema(type = "integer", format = "int64")) @RequestParam("professorId") Long professorId,
            @Parameter(name = "files", description = "첨부 파일 목록(이미지/PDF)",
                    content = @Content(array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))))
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {
        CustomUserPrincipal customPrincipal = getCustomUserPrincipal();
        Long questionId = questionService.createQuestionWithFiles(title, content, subjectId, professorId, files, customPrincipal.getUserId());
        return ResponseEntity.ok(questionId);
    }

    @GetMapping
    @Operation(summary = "질문 목록 조회", description = "조건에 맞는 질문 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 질문 목록을 조회했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QuestionDtos.Summary.class),
                            examples = @ExampleObject(value = "{\"content\": [{\"id\": 1, \"title\": \"질문 제목1\", \"authorNickname\": \"작성자1\", \"subjectName\": \"과목1\", \"professorName\": \"교수1\", \"createdAt\": \"2025-08-19T12:00:00\", \"hasAdoptedAnswer\": true}], \"pageable\": {\"pageNumber\": 0, \"pageSize\": 10, \"sort\": {\"empty\": false, \"sorted\": true, \"unsorted\": false}, \"offset\": 0, \"paged\": true, \"unpaged\": false}, \"last\": false, \"totalPages\": 1, \"totalElements\": 1, \"size\": 10, \"number\": 0, \"sort\": {\"empty\": false, \"sorted\": true, \"unsorted\": false}, \"first\": true, \"numberOfElements\": 1, \"empty\": false}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"BAD_REQUEST\", \"message\": \"잘못된 요청입니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"서버 내부 오류가 발생했습니다.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<PageResponse<QuestionDtos.Summary>> list(
            @Parameter(description = "과목 ID (선택)") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "교수 ID (선택)") @RequestParam(required = false) Long professorId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var res = questionQueryRepository.findSummaries(subjectId, professorId, pageable);
        return ResponseEntity.ok(PageResponse.of(res));
    }

    @GetMapping("/{id}")
    @Operation(summary = "질문 상세 조회", description = "특정 질문의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    public ResponseEntity<QuestionDtos.Detail> get(@Parameter(description = "질문 ID") @PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionDetail(id));
    }

    // 질문 상세 + 답변 목록은 Answer Feed API를 사용합니다.

    @DeleteMapping("/{id}")
    @Operation(summary = "질문 삭제", description = "특정 질문을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "질문 ID") @PathVariable Long id) {
        CustomUserPrincipal customPrincipal = getCustomUserPrincipal();
        questionService.deleteQuestion(id, customPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }

    private CustomUserPrincipal getCustomUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return (CustomUserPrincipal) authentication.getPrincipal();
    }
}
