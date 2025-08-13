package com.qandding.answer.repository;

import com.qandding.answer.domain.AnswerPost;
import com.qandding.question.domain.QuestionPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerPostRepository extends JpaRepository<AnswerPost, Long> {
	@EntityGraph(attributePaths = {"user", "questionPost", "aiAnswer"})
	Page<AnswerPost> findByQuestionPost(QuestionPost questionPost, Pageable pageable);
}
