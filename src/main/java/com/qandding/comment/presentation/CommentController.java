package com.qandding.comment.presentation;

import com.qandding.common.paging.PageResponse;
import com.qandding.common.error.BusinessException;
import com.qandding.common.error.ErrorCode;
import com.qandding.answer.domain.AnswerPost;
import com.qandding.answer.repository.AnswerPostRepository;
import com.qandding.comment.domain.Comment;
import com.qandding.comment.domain.CommentImage;
import com.qandding.comment.presentation.dto.CommentDtos;
import com.qandding.comment.repository.CommentImageRepository;
import com.qandding.comment.repository.CommentQueryRepository;
import com.qandding.comment.repository.CommentRepository;
import com.qandding.security.CustomUserPrincipal;
import com.qandding.user.domain.User;
import com.qandding.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
	private final CommentRepository commentRepository;
	private final CommentImageRepository commentImageRepository;
	private final CommentQueryRepository commentQueryRepository;
	private final AnswerPostRepository answerPostRepository;
	private final UserRepository userRepository;

	public CommentController(CommentRepository commentRepository, CommentImageRepository commentImageRepository,
	                         CommentQueryRepository commentQueryRepository,
	                         AnswerPostRepository answerPostRepository, UserRepository userRepository) {
		this.commentRepository = commentRepository;
		this.commentImageRepository = commentImageRepository;
		this.commentQueryRepository = commentQueryRepository;
		this.answerPostRepository = answerPostRepository;
		this.userRepository = userRepository;
	}

	public record CreateCommentRequest(
			Long answerPostId,
			@NotBlank String content,
			java.util.List<String> imageUrls
	) {}

	@PostMapping
	@Transactional
	public ResponseEntity<Long> create(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @RequestBody CreateCommentRequest req) {
		User user = userRepository.findById(principal.getUserId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		AnswerPost answer = answerPostRepository.findById(req.answerPostId()).orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
		Comment comment = commentRepository.save(new Comment(answer, user, req.content()));
		if (req.imageUrls() != null) {
			int i = 0;
			for (String url : req.imageUrls()) {
				commentImageRepository.save(new CommentImage(comment, url, i++));
			}
		}
		return ResponseEntity.ok(comment.getId());
	}

	@GetMapping
	public ResponseEntity<PageResponse<CommentDtos.Summary>> list(@RequestParam Long answerPostId,
	                                                @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		var page = commentQueryRepository.findSummaries(answerPostId, pageable);
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@DeleteMapping("/{id}")
	@Transactional
	public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long id) {
		Comment comment = commentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
		if (!comment.getUser().getId().equals(principal.getUserId())) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		commentRepository.delete(comment);
		return ResponseEntity.noContent().build();
	}
}
