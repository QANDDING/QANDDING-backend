package com.qandding.domain.answer.repository;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.question.entity.QuestionPost;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnswerPostRepository extends JpaRepository<AnswerPost, Long> {
	@EntityGraph(attributePaths = {"user", "questionPost", "aiAnswer"})
	Page<AnswerPost> findByQuestionPost(QuestionPost questionPost, Pageable pageable);

	AnswerPost findByAiAnswer_Id(Long aiAnswerId);

    AnswerPost findTopByUser_IdAndQuestionPost_IdAndTitleAndContentOrderByIdDesc(
        Long userId, Long questionPostId, String title, String content
    );

    AnswerPost findFirstByQuestionPost_IdAndAiAnswerIsNotNull(Long questionPostId);

    @EntityGraph(attributePaths = {"user", "questionPost", "aiAnswer"})
    Page<AnswerPost> findByQuestionPost_IdAndAiAnswerIsNull(Long questionPostId, Pageable pageable);

    AnswerPost findTopByQuestionPost_IdAndAiAnswerIsNotNullOrderByCreatedAtDesc(Long questionPostId);

    boolean existsByQuestionPost_IdAndAiAnswerIsNotNullAndCreatedAtAfter(Long questionPostId, java.time.LocalDateTime cutoff);
}
