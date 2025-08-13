package com.qandding.answer.repository;

import com.qandding.answer.domain.AnswerSelection;
import com.qandding.question.domain.QuestionPost;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerSelectionRepository extends JpaRepository<AnswerSelection, Long> {
	Optional<AnswerSelection> findByQuestionPost(QuestionPost questionPost);
}
