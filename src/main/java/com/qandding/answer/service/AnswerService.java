package com.qandding.answer.service;

import com.qandding.ai.domain.AiAnswer;
import com.qandding.common.error.BusinessException;
import com.qandding.common.error.ErrorCode;
import com.qandding.answer.domain.AnswerPost;
import com.qandding.answer.domain.AnswerSelection;
import com.qandding.answer.presentation.dto.AnswerDtos;
import com.qandding.answer.repository.AnswerPostRepository;
import com.qandding.answer.repository.AnswerQueryRepository;
import com.qandding.answer.repository.AnswerSelectionRepository;
import com.qandding.question.domain.QuestionPost;
import com.qandding.question.repository.QuestionPostRepository;
import com.qandding.user.domain.User;
import com.qandding.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnswerService {
	private final AnswerPostRepository answerPostRepository;
	private final AnswerSelectionRepository selectionRepository;
	private final AnswerQueryRepository answerQueryRepository;
	private final QuestionPostRepository questionPostRepository;
	private final UserRepository userRepository;

	public AnswerService(AnswerPostRepository answerPostRepository,
	                    AnswerSelectionRepository selectionRepository,
	                    AnswerQueryRepository answerQueryRepository,
	                    QuestionPostRepository questionPostRepository,
	                    UserRepository userRepository) {
		this.answerPostRepository = answerPostRepository;
		this.selectionRepository = selectionRepository;
		this.answerQueryRepository = answerQueryRepository;
		this.questionPostRepository = questionPostRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public Long createAnswer(Long userId, Long questionPostId, String aiTitle, String aiContent, String title, String content) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		QuestionPost question = questionPostRepository.findById(questionPostId).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		AiAnswer ai = null;
		if (aiTitle != null && aiContent != null) {
			ai = new AiAnswer(aiTitle, aiContent);
		}
		AnswerPost post = answerPostRepository.save(new AnswerPost(user, question, ai, title, content));
		return post.getId();
	}

	public Page<AnswerDtos.Summary> listAnswers(Long questionPostId, Pageable pageable) {
		return answerQueryRepository.findSummaries(questionPostId, pageable);
	}

	@Transactional
	public void selectAnswer(Long principalUserId, Long answerId) {
		AnswerPost answer = answerPostRepository.findById(answerId).orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
		QuestionPost question = answer.getQuestionPost();
		if (!question.getUser().getId().equals(principalUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		selectionRepository.findByQuestionPost(question).ifPresent(selectionRepository::delete);
		selectionRepository.save(new AnswerSelection(question, answer));
	}
}



