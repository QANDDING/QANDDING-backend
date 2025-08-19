package com.qandding.domain.subject.controller;

import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.professor.repository.ProfessorRepository;
import com.qandding.domain.subject.entity.Subject;
import com.qandding.domain.subject.repository.SubjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseEntity<List<SubjectSummary>> searchSubjects(
            @Parameter(description = "검색어") @RequestParam("query") String query
    ) {
        List<Subject> subjects = subjectRepository.findByNameContainingIgnoreCase(query);
        List<SubjectSummary> mapped = subjects.stream().map(SubjectSummary::from).toList();
        return ResponseEntity.ok(mapped);
    }

    @GetMapping("/professors/by-subject")
    @Operation(summary = "과목별 교수 목록", description = "선택된 과목을 담당하는 교수 목록을 조회합니다.")
    public ResponseEntity<List<ProfessorSummary>> listProfessorsBySubject(
            @Parameter(description = "과목 ID") @RequestParam("subjectId") Long subjectId
    ) {
        List<Professor> profs = professorRepository.findBySubjectId(subjectId);
        List<ProfessorSummary> mapped = profs.stream().map(ProfessorSummary::from).toList();
        return ResponseEntity.ok(mapped);
    }
}
