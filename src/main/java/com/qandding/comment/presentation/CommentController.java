package com.qandding.comment.presentation;

import com.qandding.common.paging.PageResponse;
import com.qandding.comment.presentation.dto.CommentDtos;
import com.qandding.comment.service.CommentService;
import com.qandding.security.CustomUserPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	public record CreateCommentRequest(
			Long answerPostId,
			@NotBlank String content,
			java.util.List<String> imageUrls
	) {}

	@PostMapping
	public ResponseEntity<Long> create(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @RequestBody CreateCommentRequest req) {
		Long id = commentService.createComment(principal.getUserId(), req.answerPostId(), req.content(), req.imageUrls());
		return ResponseEntity.ok(id);
	}

	@GetMapping
	public ResponseEntity<PageResponse<CommentDtos.Summary>> list(@RequestParam Long answerPostId,
	                                                @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		var page = commentService.listComments(answerPostId, pageable);
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long id) {
		commentService.deleteComment(principal.getUserId(), id);
		return ResponseEntity.noContent().build();
	}
}
