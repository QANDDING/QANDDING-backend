package com.qandding.answer.presentation;

import com.qandding.common.paging.PageResponse;
import com.qandding.common.error.BusinessException;
import com.qandding.common.error.ErrorCode;
import com.qandding.ai.domain.AiAnswer;
import com.qandding.answer.domain.AnswerPost;
import com.qandding.answer.domain.AnswerSelection;
import com.qandding.answer.presentation.dto.AnswerDtos;
import com.qandding.answer.repository.AnswerPostRepository;
import com.qandding.answer.repository.AnswerSelectionRepository;
import com.qandding.answer.repository.AnswerQueryRepository;
import com.qandding.question.domain.QuestionPost;
import com.qandding.question.repository.QuestionPostRepository;
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
@RequestMapping("/api/answers")
public class AnswerController {
	private final AnswerPostRepository answerPostRepository;
	private final AnswerSelectionRepository selectionRepository;
	private final AnswerQueryRepository answerQueryRepository;
	private final QuestionPostRepository questionPostRepository;
	private final UserRepository userRepository;

	public AnswerController(AnswerPostRepository answerPostRepository, AnswerSelectionRepository selectionRepository,
	                        AnswerQueryRepository answerQueryRepository,
	                        QuestionPostRepository questionPostRepository, UserRepository userRepository) {
		this.answerPostRepository = answerPostRepository;
		this.selectionRepository = selectionRepository;
		this.answerQueryRepository = answerQueryRepository;
		this.questionPostRepository = questionPostRepository;
		this.userRepository = userRepository;
	}

	public record CreateAnswerRequest(
			Long questionPostId,
			String aiTitle,
			String aiContent,
			@NotBlank String title,
			@NotBlank String content
	) {}

	@PostMapping
	@Transactional
	public ResponseEntity<Long> create(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @RequestBody CreateAnswerRequest req) {
		User user = userRepository.findById(principal.getUserId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		QuestionPost question = questionPostRepository.findById(req.questionPostId()).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		AiAnswer ai = null;
		if (req.aiTitle() != null && req.aiContent() != null) {
			ai = new AiAnswer(req.aiTitle(), req.aiContent());
		}
		AnswerPost post = answerPostRepository.save(new AnswerPost(user, question, ai, req.title(), req.content()));
		return ResponseEntity.ok(post.getId());
	}

	@GetMapping
	public ResponseEntity<PageResponse<AnswerDtos.Summary>> list(@RequestParam Long questionPostId,
	                                                    @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		var page = answerQueryRepository.findSummaries(questionPostId, pageable);
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@PostMapping("/{answerId}/select")
	@Transactional
	public ResponseEntity<Void> select(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @PathVariable Long answerId) {
		AnswerPost answer = answerPostRepository.findById(answerId).orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
		QuestionPost question = answer.getQuestionPost();
		if (!question.getUser().getId().equals(principal.getUserId())) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		selectionRepository.findByQuestionPost(question).ifPresent(selectionRepository::delete);
		selectionRepository.save(new AnswerSelection(question, answer));
		return ResponseEntity.noContent().build();
	}
}
