package com.qandding.domain.comment.repository;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.comment.entity.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
	@EntityGraph(attributePaths = {"user", "answerPost", "aiAnswer"})
	Page<Comment> findByAnswerPost(AnswerPost answerPost, Pageable pageable);
}
