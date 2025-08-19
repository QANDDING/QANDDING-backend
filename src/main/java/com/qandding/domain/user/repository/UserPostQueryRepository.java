package com.qandding.domain.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.question.entity.QuestionPost;

public interface UserPostQueryRepository {
    
    /**
     * 사용자가 작성한 질문글을 페이징하여 조회
     */
    Page<QuestionPost> findQuestionPostsByUserId(Long userId, Pageable pageable);
    
    /**
     * 사용자가 작성한 답변글을 페이징하여 조회
     */
    Page<AnswerPost> findAnswerPostsByUserId(Long userId, Pageable pageable);
}
