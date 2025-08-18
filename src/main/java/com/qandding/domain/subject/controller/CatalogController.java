package com.qandding.domain.subject.controller;

import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.professor.repository.ProfessorRepository;
import com.qandding.domain.subject.entity.Subject;
import com.qandding.domain.subject.repository.SubjectRepository;
import com.qandding.global.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<PageResponse<SubjectSummary>> searchSubjects(
            @Parameter(description = "검색어") @RequestParam("query") String query,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Subject> subjects = subjectRepository.findByNameContainingIgnoreCase(query, pageable);
        Page<SubjectSummary> mapped = subjects.map(SubjectSummary::from);
        return ResponseEntity.ok(PageResponse.of(mapped));
    }

    @GetMapping("/professors/by-subject")
    @Operation(summary = "과목별 교수 목록", description = "선택된 과목을 담당하는 교수 목록을 조회합니다.")
    public ResponseEntity<PageResponse<ProfessorSummary>> listProfessorsBySubject(
            @Parameter(description = "과목 ID") @RequestParam("subjectId") Long subjectId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Professor> profs = professorRepository.findBySubjectId(subjectId, pageable);
        Page<ProfessorSummary> mapped = profs.map(ProfessorSummary::from);
        return ResponseEntity.ok(PageResponse.of(mapped));
    }
}
