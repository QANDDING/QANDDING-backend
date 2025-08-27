package com.qandding.domain.answer.repository;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.AnswerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerPostRepository extends JpaRepository<AnswerPost, Long> {

    /**
     * For AnswerFeedService: to get the most recent AI answer for a question.
     * Fetches the requester eagerly.
     */
    @EntityGraph(attributePaths = {"requester"})
    AnswerPost findTopByQuestionPost_IdAndAnswerTypeOrderByCreatedAtDesc(Long questionPostId, AnswerType answerType);

    /**
     * For AnswerFeedService: to get a paginated list of user answers for a question.
     * Fetches the author eagerly.
     */
    @EntityGraph(attributePaths = {"author"})
    Page<AnswerPost> findByQuestionPost_IdAndAnswerType(Long questionPostId, AnswerType answerType, Pageable pageable);

}