package com.qandding.answer.presentation;

import com.qandding.common.paging.PageResponse;
import com.qandding.answer.presentation.dto.AnswerDtos;
import com.qandding.answer.service.AnswerService;
import com.qandding.security.CustomUserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/answers")
public class AnswerController {
	private final AnswerService answerService;

	public AnswerController(AnswerService answerService) {
		this.answerService = answerService;
	}

	public record CreateAnswerRequest(
			Long questionPostId,
			String aiTitle,
			String aiContent,
			@NotBlank String title,
			@NotBlank String content
	) {}

	@PostMapping
	public ResponseEntity<Long> create(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @RequestBody CreateAnswerRequest req) {
		Long id = answerService.createAnswer(principal.getUserId(), req.questionPostId(), req.aiTitle(), req.aiContent(), req.title(), req.content());
		return ResponseEntity.ok(id);
	}

	@GetMapping
	public ResponseEntity<PageResponse<AnswerDtos.Summary>> list(@RequestParam Long questionPostId,
	                                                    @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		var page = answerService.listAnswers(questionPostId, pageable);
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@PostMapping("/{answerId}/select")
	public ResponseEntity<Void> select(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @PathVariable Long answerId) {
		answerService.selectAnswer(principal.getUserId(), answerId);
		return ResponseEntity.noContent().build();
	}
}
