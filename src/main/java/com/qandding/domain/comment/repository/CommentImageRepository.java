package com.qandding.domain.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qandding.domain.comment.entity.CommentImage;

public interface CommentImageRepository extends JpaRepository<CommentImage, Long> {
}
