package com.qandding.domain.ai.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qandding.domain.ai.entity.AiAnswer;

public interface AiAnswerRepository extends JpaRepository<AiAnswer, Long> {
    
    /**
     * 특정 질문에 대한 AI 답변들을 페이징하여 조회
     */
    @Query("SELECT a FROM AiAnswer a JOIN AnswerPost ap ON a.id = ap.aiAnswer.id WHERE ap.questionPost.id = :questionPostId")
    Page<AiAnswer> findByQuestionPostId(@Param("questionPostId") Long questionPostId, Pageable pageable);
    
    /**
     * 특정 질문에 대한 AI 답변 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AiAnswer a JOIN AnswerPost ap ON a.id = ap.aiAnswer.id WHERE ap.questionPost.id = :questionPostId")
    long countByQuestionPostId(@Param("questionPostId") Long questionPostId);
}
