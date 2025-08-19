package com.qandding.domain.comment.repository;

import static com.qandding.domain.comment.entity.QComment.comment;
import static com.qandding.domain.user.entity.QUser.user;

import com.qandding.domain.comment.dto.CommentDtos;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.qandding.global.storage.S3PresignService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class CommentQueryRepository {
	private final JPAQueryFactory query;
	private final CommentImageRepository commentImageRepository;
    private final S3PresignService s3PresignService;

	public CommentQueryRepository(JPAQueryFactory query, CommentImageRepository commentImageRepository, S3PresignService s3PresignService) {
		this.query = query;
		this.commentImageRepository = commentImageRepository;
        this.s3PresignService = s3PresignService;
	}

	public Page<CommentDtos.Summary> findSummaries(Long answerPostId, Pageable pageable) {
		var baseQuery = query
			.select(Projections.constructor(CommentDtos.Summary.class,
				comment.id,
				user.nickname,
				comment.content,
				comment.createdAt
			))
			.from(comment)
			.join(comment.user, user)
			.where(comment.answerPost.id.eq(answerPostId));

		long total = baseQuery.fetch().size();
		List<CommentDtos.Summary> base = baseQuery
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(comment.id.desc())
			.fetch();

		// 이미지 일괄 조회 및 매핑
		List<Long> ids = base.stream().map(CommentDtos.Summary::getId).toList();
		Map<Long, List<String>> imageMap = new HashMap<>();
		if (!ids.isEmpty()) {
			var images = commentImageRepository.findByCommentIdInOrderBySortOrderAsc(ids);
			for (var ci : images) {
                String presigned = s3PresignService.presignGetUrlByUrl(ci.getUrl());
				imageMap.computeIfAbsent(ci.getComment().getId(), k -> new ArrayList<>()).add(presigned);
			}
		}

		List<CommentDtos.Summary> content = base.stream()
			.map(s -> new CommentDtos.Summary(
				s.getId(), s.getNickname(), s.getContent(), s.getCreatedAt(),
				imageMap.getOrDefault(s.getId(), List.of())
			))
			.collect(Collectors.toList());

		return new PageImpl<>(content, pageable, total);
	}
}
