package com.qandding.comment.repository;

import com.qandding.comment.domain.CommentImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentImageRepository extends JpaRepository<CommentImage, Long> {
}
