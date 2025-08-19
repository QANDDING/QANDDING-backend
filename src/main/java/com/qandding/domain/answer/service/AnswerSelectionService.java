package com.qandding.domain.answer.service;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.AnswerSelection;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.answer.repository.AnswerSelectionRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerSelectionService {

    private final AnswerSelectionRepository answerSelectionRepository;
    private final AnswerPostRepository answerPostRepository;
    private final QuestionPostRepository questionPostRepository;

    @Transactional
    public void select(Long answerPostId, Long userId) {
        AnswerPost answerPost = answerPostRepository.findById(answerPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        // AI 답변은 채택 불가
        if (answerPost.getAiAnswer() != null) {
            throw new BusinessException(ErrorCode.ANSWER_NOT_SELECTABLE, "AI 답변은 채택할 수 없습니다.");
        }
        QuestionPost question = answerPost.getQuestionPost();
        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }

        var existing = answerSelectionRepository.findByQuestionPost(question);
        if (existing.isPresent()) {
            AnswerSelection sel = existing.get();
            if (!sel.getAnswerPost().getId().equals(answerPostId)) {
                sel.changeAnswer(answerPost);
            }
        } else {
            answerSelectionRepository.save(new AnswerSelection(question, answerPost));
        }
    }

    @Transactional
    public void unselect(Long questionPostId, Long userId) {
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }
        answerSelectionRepository.findByQuestionPost(question)
                .ifPresent(answerSelectionRepository::delete);
    }
}
