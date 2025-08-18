package com.qandding.domain.answer.repository;

import com.qandding.domain.answer.entity.UserAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
	List<UserAnswer> findByQuestionPostIdOrderByCreatedAtDesc(Long questionPostId);
	
	// 유저별 답변 조회
	List<UserAnswer> findByUserIdOrderByCreatedAtDesc(Long userId);
	
	// 유저별 답변 페이징 조회
	Page<UserAnswer> findByUserId(Long userId, Pageable pageable);
	
	// 유저별 + 질문별 답변 조회
	List<UserAnswer> findByUserIdAndQuestionPostIdOrderByCreatedAtDesc(Long userId, Long questionPostId);
}
