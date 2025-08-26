package com.qandding.domain.question.controller;

import com.qandding.domain.question.dto.QuestionCreateRequest;
import com.qandding.domain.question.dto.QuestionDtos;
import com.qandding.domain.question.dto.QuestionStatusFilter;
import com.qandding.domain.question.repository.QuestionQueryRepository;
import com.qandding.domain.question.service.QuestionService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.paging.PageResponse;
import com.qandding.global.common.response.CommonResponse;
import com.qandding.global.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Tag(name = "Question", description = "질문 관련 API")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionQueryRepository questionQueryRepository;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "질문 생성 (Presigned URL 방식)", description = "S3에 선업로드 후, 파일 URL과 함께 질문 내용을 JSON으로 받아 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "질문 생성 성공",
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
        @ApiResponse(responseCode = "404", description = "관련 정보(사용자, 과목, 교수)를 찾을 수 없음")
    })
    public ResponseEntity<CommonResponse<Long>> create(
            @RequestBody @Valid QuestionCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.info("질문 생성 요청 - userId: {}, title: {}", customPrincipal.getUserId(), request.getTitle());
        Long questionId = questionService.createQuestion(
            request.getTitle(),
            request.getContent(),
            request.getSubjectId(),
            request.getProfessorId(),
            request.getImageUrls(),
            customPrincipal.getUserId()
        );
        log.info("질문 생성 완료 - questionId: {}, userId: {}", questionId, customPrincipal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(ResponseCode.CREATED, questionId));
    }

    @GetMapping
    @Operation(summary = "질문 목록 조회", description = "조건에 맞는 질문 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value =
                    "{\n" +
                    "  \"content\": [\n" +
                    "    {\n" +
                    "      \"id\": 1,\n" +
                    "      \"title\": \"이 문제 어떻게 푸나요?\",\n" +
                    "      \"authorNickname\": \"학생1\",\n" +
                    "      \"subjectName\": \"미적분학\",\n" +
                    "      \"professorName\": \"김교수\",\n" +
                    "      \"createdAt\": \"2025-08-27T10:00:00\",\n" +
                    "      \"hasAiAnswer\": true,\n" +
                    "      \"hasMemberAnswer\": false,\n" +
                    "      \"isAdopted\": false\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"id\": 2,\n" +
                    "      \"title\": \"2번 문제 질문입니다.\",\n" +
                    "      \"authorNickname\": \"학생2\",\n" +
                    "      \"subjectName\": \"미적분학\",\n" +
                    "      \"professorName\": \"김교수\",\n" +
                    "      \"createdAt\": \"2025-08-27T11:00:00\",\n" +
                    "      \"hasAiAnswer\": true,\n" +
                    "      \"hasMemberAnswer\": true,\n" +
                    "      \"isAdopted\": true\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"page\": 0,\n" +
                    "  \"size\": 10,\n" +
                    "  \"totalElements\": 2,\n" +
                    "  \"totalPages\": 1,\n" +
                    "  \"last\": true\n" +
                    "}"))
        ),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<PageResponse<QuestionDtos.Summary>> list(
            @Parameter(description = "과목 ID (선택)") @RequestParam(required = false) Long subjectId,
            @Parameter(description = "교수 ID (선택)") @RequestParam(required = false) Long professorId,
            @Parameter(description = "검색 키워드 (제목/내용)") @RequestParam(required = false) String keyword,
            @Parameter(description = "조회 상태 필터 (ALL, ANSWERED, UNANSWERED, ADOPTED)") @RequestParam(required = false, defaultValue = "ALL") QuestionStatusFilter status,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("list() 호출됨 - subjectId: {}, professorId: {}, keyword: {}, status: {}, page: {}, size: {}", subjectId, professorId, keyword, status, page, size);
        // 이 API는 인증이 필요 없으므로 @AuthenticationPrincipal을 받지 않음 (의도된 설계)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var res = questionQueryRepository.findSummaries(subjectId, professorId, keyword, status, pageable);
        log.debug("list() 반환 - PageResponse: {}개 항목", res.getTotalElements());
        return ResponseEntity.ok(PageResponse.of(res));
    }

    

    @GetMapping("/{id}")
    @Operation(summary = "질문 상세 조회", description = "특정 질문의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = QuestionDtos.Detail.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\n  \"id\": 1,\n  \"title\": \"미적분학 1번 문제 질문입니다.\",\n  \"content\": \"이 문제의 풀이 과정이 이해가 안 됩니다. 특히 이 부분(사진)이요.\",\n  \"authorNickname\": \"질문자1\",\n  \"subjectName\": \"미적분학\",\n  \"professorName\": \"김교수\",\n  \"imageUrls\": [\"https://example.com/image1.jpg\", \"https://example.com/image2.png\"],\n  \"createdAt\": \"2025-08-27T10:00:00\",\n  \"updatedAt\": \"2025-08-27T10:05:00\",\n  \"hasAiAnswer\": true,\n  \"hasMemberAnswer\": false,\n  \"isAdopted\": false\n}"))
        ),
        @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<QuestionDtos.Detail> get(@Parameter(description = "질문 ID") @PathVariable Long id) {
        log.debug("get() 호출됨 - id: {}", id);
        // 이 API는 인증이 필요 없으므로 @AuthenticationPrincipal을 받지 않음 (의도된 설계)
        QuestionDtos.Detail questionDetail = questionService.getQuestionDetail(id);
        log.debug("get() 반환 - questionId: {}", questionDetail.getId());
        return ResponseEntity.ok(questionDetail);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "질문 삭제", description = "특정 질문을 삭제합니다.")
        @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "질문 삭제 성공",
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
        @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Long>> delete(
            @Parameter(description = "질문 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal
    ) {
        log.debug("delete() 호출됨 - id: {}, userId: {}", id, customPrincipal.getUserId());
        // 인증 확인 및 예외 처리 로직 제거
        log.info("질문 삭제 요청 - questionId: {}, userId: {}", id, customPrincipal.getUserId());
        questionService.deleteQuestion(id, customPrincipal.getUserId());
        log.info("질문 삭제 완료 - questionId: {}, userId: {}", id, customPrincipal.getUserId());
        log.debug("delete() 반환 - void");
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.NO_CONTENT, id));
    }
}