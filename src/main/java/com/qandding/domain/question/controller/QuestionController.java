package com.qandding.domain.question.controller;

import com.qandding.domain.question.dto.QuestionDtos;
import com.qandding.domain.question.repository.QuestionQueryRepository;
import com.qandding.domain.question.service.QuestionService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @Parameter(name = "files", description = "첨부 파일 목록(이미지/PDF)", content = @Content(array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))))
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) throws IOException {
        log.debug("create() 호출됨 - title: {}, subjectId: {}, professorId: {}, userId: {}", title, subjectId, professorId, customPrincipal.getUserId());
        // 인증 확인 및 예외 처리 로직 제거 -> 스프링 시큐리티와 @RestControllerAdvice가 담당
        log.info("질문 생성 요청 - userId: {}, title: {}", customPrincipal.getUserId(), title);
        Long questionId = questionService.createQuestionWithFiles(title, content, subjectId, professorId, files, customPrincipal.getUserId());
        log.info("질문 생성 완료 - questionId: {}, userId: {}", questionId, customPrincipal.getUserId());
        log.debug("create() 반환 - questionId: {}", questionId);
        return ResponseEntity.ok(questionId);
    }

    @GetMapping
    @Operation(summary = "질문 목록 조회", description = "조건에 맞는 질문 목록을 페이징하여 조회합니다.")
    // ... (@ApiResponses는 기존과 동일하게 유지)
    public ResponseEntity<PageResponse<QuestionDtos.Summary>> list(
            @Parameter(description = "과목 ID (선택)") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "교수 ID (선택)") @RequestParam(required = false) Long professorId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("list() 호출됨 - subjectId: {}, professorId: {}, page: {}, size: {}", subjectId, professorId, page, size);
        // 이 API는 인증이 필요 없으므로 @AuthenticationPrincipal을 받지 않음 (의도된 설계)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var res = questionQueryRepository.findSummaries(subjectId, professorId, pageable);
        log.debug("list() 반환 - PageResponse: {}개 항목", res.getTotalElements());
        return ResponseEntity.ok(PageResponse.of(res));
    }

    

    @GetMapping("/{id}")
    @Operation(summary = "질문 상세 조회", description = "특정 질문의 상세 정보를 조회합니다.")
    // ... (@ApiResponses는 기존과 동일하게 유지)
    public ResponseEntity<QuestionDtos.Detail> get(@Parameter(description = "질문 ID") @PathVariable Long id) {
        log.debug("get() 호출됨 - id: {}", id);
        // 이 API는 인증이 필요 없으므로 @AuthenticationPrincipal을 받지 않음 (의도된 설계)
        QuestionDtos.Detail questionDetail = questionService.getQuestionDetail(id);
        log.debug("get() 반환 - questionId: {}", questionDetail.getQuestionId());
        return ResponseEntity.ok(questionDetail);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "질문 삭제", description = "특정 질문을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "질문 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.debug("delete() 호출됨 - id: {}, userId: {}", id, customPrincipal.getUserId());
        // 인증 확인 및 예외 처리 로직 제거
        log.info("질문 삭제 요청 - questionId: {}, userId: {}", id, customPrincipal.getUserId());
        questionService.deleteQuestion(id, customPrincipal.getUserId());
        log.info("질문 삭제 완료 - questionId: {}, userId: {}", id, customPrincipal.getUserId());
        log.debug("delete() 반환 - void");
        return ResponseEntity.noContent().build();
    }
}