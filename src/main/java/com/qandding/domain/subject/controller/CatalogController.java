package com.qandding.domain.subject.controller;

import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.professor.repository.ProfessorRepository;
import com.qandding.domain.subject.entity.Subject;
import com.qandding.domain.subject.repository.SubjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.qandding.global.common.error.ApiErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "과목/교수 검색 API")
public class CatalogController {

    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;

    public record SubjectSummary(Long id, String name) {
        public static SubjectSummary from(Subject s) { return new SubjectSummary(s.getId(), s.getName()); }
    }

    public record ProfessorSummary(Long id, String name) {
        public static ProfessorSummary from(Professor p) { return new ProfessorSummary(p.getId(), p.getName()); }
    }

        @GetMapping("/subjects/search")
    @Operation(summary = "과목 검색", description = "이름 일부로 과목을 검색합니다. 예: '경제' → '경제학개론' 등")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 과목 목록을 조회했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubjectSummary.class),
                            examples = @ExampleObject(value = "[{\"id\": 1, \"name\": \"Subject1\"}, {\"id\": 2, \"name\": \"Subject2\"}]"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"BAD_REQUEST\", \"message\": \"Bad request.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Internal server error.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<List<SubjectSummary>> searchSubjects(
            @Parameter(description = "검색어") @RequestParam("query") String query
    ) {
        List<Subject> subjects = subjectRepository.findByNameContainingIgnoreCase(query);
        List<SubjectSummary> mapped = subjects.stream().map(SubjectSummary::from).toList();
        return ResponseEntity.ok(mapped);
    }

    @GetMapping("/professors/by-subject")
    @Operation(summary = "과목별 교수 목록", description = "선택된 과목을 담당하는 교수 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 교수 목록을 조회했습니다.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProfessorSummary.class),
                            examples = @ExampleObject(value = "[{\"id\": 101, \"name\": \"Professor Kim\"}, {\"id\": 102, \"name\": \"Professor Lee\"}]"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"BAD_REQUEST\", \"message\": \"Bad request.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}"))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"Internal server error.\", \"timestamp\": \"2025-08-19T10:00:00.000+00:00\", \"errors\": []}")))
    })
    public ResponseEntity<List<ProfessorSummary>> listProfessorsBySubject(
            @Parameter(description = "과목 ID") @RequestParam("subjectId") Long subjectId
    ) {
        List<Professor> profs = professorRepository.findBySubjectId(subjectId);
        List<ProfessorSummary> mapped = profs.stream().map(ProfessorSummary::from).toList();
        return ResponseEntity.ok(mapped);
    }
}
