package com.qandding.domain.answer.repository;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.question.entity.QuestionPost;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerPostRepository extends JpaRepository<AnswerPost, Long> {
	@EntityGraph(attributePaths = {"user", "questionPost", "aiAnswer"})
	Page<AnswerPost> findByQuestionPost(QuestionPost questionPost, Pageable pageable);
}
