package com.qandding.question.presentation;

import com.qandding.common.paging.PageResponse;
import com.qandding.question.presentation.dto.QuestionDtos;
import com.qandding.question.service.QuestionService;
import com.qandding.security.CustomUserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
	private final QuestionService questionService;

	public QuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	public record CreateQuestionRequest(
			@NotBlank String title,
			@NotBlank String content,
			Long subjectId,
			Long professorId,
			List<String> imageUrls
	) {}

	@PostMapping
	public ResponseEntity<Long> create(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @RequestBody CreateQuestionRequest req) {
		Long id = questionService.createQuestion(
				principal.getUserId(),
				req.subjectId(),
				req.professorId(),
				req.title(),
				req.content(),
				req.imageUrls()
		);
		return ResponseEntity.ok(id);
	}

	@GetMapping
	public ResponseEntity<PageResponse<QuestionDtos.Summary>> list(
			@RequestParam(required = false) Long subjectId,
			@RequestParam(required = false) Long professorId,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		var page = questionService.findQuestions(subjectId, professorId, pageable);
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@GetMapping("/{id}")
	public ResponseEntity<QuestionDtos.Detail> get(@PathVariable Long id) {
		return ResponseEntity.ok(questionService.getQuestionDetail(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long id) {
		questionService.deleteQuestion(principal.getUserId(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/match/subject/{subjectId}")
	public ResponseEntity<Boolean> matchSubject(@PathVariable Long id, @PathVariable Long subjectId) {
		return ResponseEntity.ok(questionService.matchesSubject(id, subjectId));
	}

	@GetMapping("/{id}/match/professor/{professorId}")
	public ResponseEntity<Boolean> matchProfessor(@PathVariable Long id, @PathVariable Long professorId) {
		return ResponseEntity.ok(questionService.matchesProfessor(id, professorId));
	}
}
