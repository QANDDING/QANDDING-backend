package com.qandding.domain.question.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.qandding.domain.question.entity.QuestionImage;

public interface QuestionImageRepository extends JpaRepository<QuestionImage, Long> {
	List<QuestionImage> findByQuestionPostIdOrderBySortOrderAsc(Long questionPostId);
}
