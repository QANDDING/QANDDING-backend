package com.qandding.domain.comment.repository;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.comment.entity.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"user", "answerPost"})
    Page<Comment> findByAnswerPost(AnswerPost answerPost, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findByAnswerPost_IdAndParentIsNull(Long answerPostId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    java.util.List<Comment> findByParent_IdInOrderByCreatedAtAsc(java.util.Collection<Long> parentIds);
}
