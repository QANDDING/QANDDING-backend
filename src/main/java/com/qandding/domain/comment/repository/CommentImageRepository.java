package com.qandding.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

import com.qandding.domain.comment.entity.CommentImage;

public interface CommentImageRepository extends JpaRepository<CommentImage, Long> {
    List<CommentImage> findByCommentIdOrderBySortOrderAsc(Long commentId);
    List<CommentImage> findByCommentIdInOrderBySortOrderAsc(Collection<Long> commentIds);
}
