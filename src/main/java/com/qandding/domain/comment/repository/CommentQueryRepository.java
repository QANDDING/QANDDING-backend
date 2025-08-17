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

import java.util.List;

@Repository
public class CommentQueryRepository {
	private final JPAQueryFactory query;

	public CommentQueryRepository(JPAQueryFactory query) {
		this.query = query;
	}

	public Page<CommentDtos.Summary> findSummaries(Long answerPostId, Pageable pageable) {
		var base = query
			.select(Projections.constructor(CommentDtos.Summary.class,
				comment.id,
				user.nickname,
				comment.content,
				comment.createdAt
			))
			.from(comment)
			.join(comment.user, user)
			.where(comment.answerPost.id.eq(answerPostId));

		long total = base.fetch().size();
		List<CommentDtos.Summary> content = base
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(comment.id.desc())
			.fetch();
		return new PageImpl<>(content, pageable, total);
	}
}
