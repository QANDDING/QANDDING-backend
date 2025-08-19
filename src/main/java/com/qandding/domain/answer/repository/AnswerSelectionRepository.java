package com.qandding.domain.answer.repository;

import com.qandding.domain.answer.entity.AnswerSelection;
import com.qandding.domain.question.entity.QuestionPost;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerSelectionRepository extends JpaRepository<AnswerSelection, Long> {
	Optional<AnswerSelection> findByQuestionPost(QuestionPost questionPost);

    // Convenience method to avoid loading QuestionPost entity
    Optional<AnswerSelection> findByQuestionPost_Id(Long questionPostId);
}
