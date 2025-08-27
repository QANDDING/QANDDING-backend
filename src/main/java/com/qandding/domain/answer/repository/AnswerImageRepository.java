package com.qandding.domain.answer.repository;

import com.qandding.domain.answer.entity.AnswerImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerImageRepository extends JpaRepository<AnswerImage, Long> {
  List<AnswerImage> findByAnswerPostIdOrderBySortOrderAsc(Long answerPostId);
  List<AnswerImage> findByAnswerPostIdInOrderBySortOrderAsc(Collection<Long> answerPostIds);

  void deleteAllByAnswerPostId(Long answerPostId);
}
