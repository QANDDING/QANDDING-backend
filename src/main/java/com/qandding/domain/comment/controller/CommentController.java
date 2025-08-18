package com.qandding.domain.comment.controller;

import com.qandding.global.common.paging.PageResponse;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.ai.repository.AiAnswerRepository;
import com.qandding.domain.comment.dto.CommentDtos;
import com.qandding.domain.comment.entity.Comment;
import com.qandding.domain.comment.entity.CommentImage;
import com.qandding.domain.comment.repository.CommentImageRepository;
import com.qandding.domain.comment.repository.CommentQueryRepository;
import com.qandding.domain.comment.repository.CommentRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.domain.user.entity.CustomUserPrincipal;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/comments")
public class CommentController {
	private final CommentRepository commentRepository;
	private final CommentImageRepository commentImageRepository;
	private final CommentQueryRepository commentQueryRepository;
	private final AnswerPostRepository answerPostRepository;
	private final AiAnswerRepository aiAnswerRepository;
	private final UserRepository userRepository;

	public CommentController(CommentRepository commentRepository, CommentImageRepository commentImageRepository,
	                         CommentQueryRepository commentQueryRepository,
	                         AnswerPostRepository answerPostRepository, AiAnswerRepository aiAnswerRepository, UserRepository userRepository) {
		this.commentRepository = commentRepository;
		this.commentImageRepository = commentImageRepository;
		this.commentQueryRepository = commentQueryRepository;
		this.answerPostRepository = answerPostRepository;
		this.aiAnswerRepository = aiAnswerRepository;
		this.userRepository = userRepository;
	}

	public record CreateCommentRequest(
			Long answerPostId,        // 답변 ID
			@NotBlank String content, // 댓글 내용
			Long aiAnswerId,          // AI 답변 ID (선택적, 기존 것 참조)
			java.util.List<String> imageUrls
	) {}

	@PostMapping
	@Transactional
	public ResponseEntity<Long> create(@RequestBody CreateCommentRequest req) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		log.info("Creating comment for answer: {}, user: {}, aiAnswerId: {}", 
			req.answerPostId(), customPrincipal.getUserId(), req.aiAnswerId());
		
		User user = userRepository.findById(customPrincipal.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		
		AnswerPost answer = answerPostRepository.findById(req.answerPostId())
			.orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
		
		Comment comment;
		if (req.aiAnswerId() != null) {
			// 기존 AI 답변 참조
			AiAnswer aiAnswer = aiAnswerRepository.findById(req.aiAnswerId())
				.orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
			comment = commentRepository.save(new Comment(answer, aiAnswer, user, req.content()));
			log.info("Created comment with AI answer reference: {}", comment.getId());
		} else {
			// 일반 댓글
			comment = commentRepository.save(new Comment(answer, user, req.content()));
			log.info("Created regular comment: {}", comment.getId());
		}
		
		// 이미지 처리
		if (req.imageUrls() != null && !req.imageUrls().isEmpty()) {
			log.info("Processing {} images for comment {}", req.imageUrls().size(), comment.getId());
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
		log.info("Fetching comments for answer: {}, page: {}, size: {}", 
			answerPostId, pageable.getPageNumber(), pageable.getPageSize());
		
		var page = commentQueryRepository.findSummaries(answerPostId, pageable);
		log.info("Found {} comments, total: {}", page.getContent().size(), page.getTotalElements());
		
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@DeleteMapping("/{id}")
	@Transactional
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		log.info("Attempting to delete comment with id: {}, user: {}", id, customPrincipal.getUserId());
		
		Comment comment = commentRepository.findById(id)
			.orElseThrow(() -> {
				log.error("Comment not found with id: {}", id);
				return new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
			});
		
		log.info("Found comment: id={}, author={}", comment.getId(), comment.getUser().getId());
		
		if (!comment.getUser().getId().equals(customPrincipal.getUserId())) {
			log.error("User {} is not authorized to delete comment {}", customPrincipal.getUserId(), id);
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		
		commentRepository.delete(comment);
		log.info("Successfully deleted comment {}", id);
		
		return ResponseEntity.noContent().build();
	}
}
