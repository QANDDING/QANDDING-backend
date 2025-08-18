package com.qandding.domain.answer.controller;

import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.dto.UserAnswerDtos;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.UserAnswer;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.answer.repository.AnswerQueryRepository;
import com.qandding.domain.answer.repository.UserAnswerQueryRepository;
import com.qandding.domain.answer.repository.UserAnswerRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.common.paging.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user-answers")
public class UserAnswerController {
	private final UserAnswerRepository userAnswerRepository;
	private final UserAnswerQueryRepository userAnswerQueryRepository;
	private final AnswerPostRepository answerPostRepository;
	private final AnswerQueryRepository answerQueryRepository;
	private final QuestionPostRepository questionPostRepository;
	private final UserRepository userRepository;

	public UserAnswerController(UserAnswerRepository userAnswerRepository,
	                           UserAnswerQueryRepository userAnswerQueryRepository,
	                           AnswerPostRepository answerPostRepository,
	                           AnswerQueryRepository answerQueryRepository,
	                           QuestionPostRepository questionPostRepository,
	                           UserRepository userRepository) {
		this.userAnswerRepository = userAnswerRepository;
		this.userAnswerQueryRepository = userAnswerQueryRepository;
		this.answerPostRepository = answerPostRepository;
		this.answerQueryRepository = answerQueryRepository;
		this.questionPostRepository = questionPostRepository;
		this.userRepository = userRepository;
	}

	public record CreateUserAnswerRequest(
			Long questionPostId,
			@NotBlank String title,
			@NotBlank String content
	) {}

	@PostMapping
	@Transactional
	public ResponseEntity<Long> create(@RequestBody CreateUserAnswerRequest req) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		log.info("Creating user answer for question: {}, user: {}", req.questionPostId(), customPrincipal.getUserId());
		
		User user = userRepository.findById(customPrincipal.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		
		var question = questionPostRepository.findById(req.questionPostId())
			.orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		
		// 1. UserAnswer 생성 및 저장
		UserAnswer userAnswer = userAnswerRepository.save(
			new UserAnswer(user, question, req.title(), req.content())
		);
		
		// 2. AnswerPost 생성 및 저장 (Comment 시스템을 위해)
		AnswerPost answerPost = answerPostRepository.save(
			new AnswerPost(user, question, req.title(), req.content())
		);
		
		log.info("Created user answer with id: {} and answer post with id: {}", 
			userAnswer.getId(), answerPost.getId());
		
		// UserAnswer의 ID를 반환 (기존 API 호환성 유지)
		return ResponseEntity.ok(userAnswer.getId());
	}

	@GetMapping
	public ResponseEntity<PageResponse<UserAnswerDtos.Summary>> list(
			@RequestParam Long questionPostId,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		
		log.info("Fetching user answers for question: {}, page: {}, size: {}", 
			questionPostId, pageable.getPageNumber(), pageable.getPageSize());
		
		var page = userAnswerQueryRepository.findSummaries(questionPostId, pageable);
		log.info("Found {} user answers, total: {}", page.getContent().size(), page.getTotalElements());
		
		return ResponseEntity.ok(PageResponse.of(page));
	}

	// 유저별 답변 조회
	@GetMapping("/user/{userId}")
	public ResponseEntity<PageResponse<UserAnswerDtos.Summary>> listByUser(
			@PathVariable Long userId,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		
		log.info("Fetching user answers by user: {}, page: {}, size: {}", 
			userId, pageable.getPageNumber(), pageable.getPageSize());
		
		var page = userAnswerQueryRepository.findSummariesByUserId(userId, pageable);
		log.info("Found {} user answers for user {}, total: {}", 
			page.getContent().size(), userId, page.getTotalElements());
		
		return ResponseEntity.ok(PageResponse.of(page));
	}

	// 유저별 + 질문별 답변 조회
	@GetMapping("/user/{userId}/question/{questionId}")
	public ResponseEntity<PageResponse<UserAnswerDtos.Summary>> listByUserAndQuestion(
			@PathVariable Long userId,
			@PathVariable Long questionId,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		
		log.info("Fetching user answers by user: {} and question: {}, page: {}, size: {}", 
			userId, questionId, pageable.getPageNumber(), pageable.getPageSize());
		
		var page = userAnswerQueryRepository.findSummariesByUserIdAndQuestionId(userId, questionId, pageable);
		log.info("Found {} user answers for user {} and question {}, total: {}", 
			page.getContent().size(), userId, questionId, page.getTotalElements());
		
		return ResponseEntity.ok(PageResponse.of(page));
	}

	// 질문에 대한 모든 답변 통합 조회 (AI + 사용자)
	@GetMapping("/all")
	public ResponseEntity<PageResponse<AnswerDtos.Summary>> listAllAnswers(
			@RequestParam Long questionPostId,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		
		log.info("Fetching all answers (AI + User) for question: {}, page: {}, size: {}", 
			questionPostId, pageable.getPageNumber(), pageable.getPageSize());
		
		var page = answerQueryRepository.findSummaries(questionPostId, pageable);
		log.info("Found {} total answers for question {}, total: {}", 
			page.getContent().size(), questionPostId, page.getTotalElements());
		
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	public ResponseEntity<UserAnswerDtos.Detail> get(@PathVariable Long id) {
		log.info("Fetching user answer with id: {}", id);
		
		UserAnswer answer = userAnswerRepository.findById(id)
			.orElseThrow(() -> {
				log.error("User answer not found with id: {}", id);
				return new BusinessException(ErrorCode.ANSWER_NOT_FOUND);
			});
		
		log.info("Found user answer: id={}, title={}, author={}", 
			answer.getId(), answer.getTitle(), answer.getUser().getNickname());
		
		return ResponseEntity.ok(UserAnswerDtos.Detail.from(answer));
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
		
		log.info("Attempting to delete user answer with id: {}, user: {}", id, customPrincipal.getUserId());
		
		UserAnswer answer = userAnswerRepository.findById(id)
			.orElseThrow(() -> {
				log.error("User answer not found with id: {}", id);
				return new BusinessException(ErrorCode.ANSWER_NOT_FOUND);
			});
		
		log.info("Found user answer: id={}, title={}, author={}", 
			answer.getId(), answer.getTitle(), answer.getUser().getId());
		
		if (!answer.getUser().getId().equals(customPrincipal.getUserId())) {
			log.error("User {} is not authorized to delete answer {}", customPrincipal.getUserId(), id);
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		
		userAnswerRepository.deleteById(id);
		log.info("Successfully deleted user answer {}", id);
		
		return ResponseEntity.noContent().build();
	}
}
