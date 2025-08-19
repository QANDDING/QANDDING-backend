package com.qandding.domain.answer.service;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.AnswerSelection;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.answer.repository.AnswerSelectionRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnswerSelectionService {

    private final AnswerPostRepository answerPostRepository;
    private final AnswerSelectionRepository answerSelectionRepository;

    @Transactional
    public Long adopt(Long answerPostId, Long requesterUserId) {
        AnswerPost answer = answerPostRepository.findById(answerPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        // Only question owner can adopt
        Long ownerId = answer.getQuestionPost().getUser().getId();
        if (!ownerId.equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }

        // AI answers cannot be adopted
        if (answer.getAiAnswer() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // Ensure single selection per question: replace existing if different
        var existing = answerSelectionRepository.findByQuestionPost(answer.getQuestionPost());
        existing.ifPresent(answerSelectionRepository::delete);

        AnswerSelection sel = new AnswerSelection(answer.getQuestionPost(), answer);
        answerSelectionRepository.save(sel);
        return answer.getId();
    }

    @Transactional
    public void unadopt(Long questionPostId, Long requesterUserId) {
        var selectionOpt = answerSelectionRepository.findByQuestionPost_Id(questionPostId);
        if (selectionOpt.isEmpty()) {
            // Nothing to do
            return;
        }
        var sel = selectionOpt.get();

        // Only question owner can unadopt
        Long ownerId = sel.getQuestionPost().getUser().getId();
        if (!ownerId.equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }
        answerSelectionRepository.delete(sel);
    }
}

