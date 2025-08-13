package com.qandding.question.repository;

import com.qandding.question.domain.QuestionImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionImageRepository extends JpaRepository<QuestionImage, Long> {
	List<QuestionImage> findByQuestionPostIdOrderBySortOrderAsc(Long questionPostId);
}
