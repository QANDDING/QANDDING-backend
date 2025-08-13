package com.qandding.comment.repository;

import com.qandding.comment.domain.Comment;
import com.qandding.answer.domain.AnswerPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
	@EntityGraph(attributePaths = {"user", "answerPost", "aiAnswer"})
	Page<Comment> findByAnswerPost(AnswerPost answerPost, Pageable pageable);
}
