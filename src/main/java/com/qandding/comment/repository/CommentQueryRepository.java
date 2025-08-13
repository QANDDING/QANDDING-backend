package com.qandding.comment.repository;

import static com.qandding.comment.domain.QComment.comment;
import static com.qandding.user.domain.QUser.user;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.qandding.comment.presentation.dto.CommentDtos;
import com.qandding.answer.domain.QAnswerPost;
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
		QAnswerPost a = QAnswerPost.answerPost;
		var base = query
			.select(Projections.constructor(CommentDtos.Summary.class,
				comment.id,
				user.nickname,
				comment.content,
				comment.createdAt
			))
			.from(comment)
			.join(comment.user, user)
			.join(comment.answerPost, a)
			.where(a.id.eq(answerPostId));

		long total = base.fetch().size();
		List<CommentDtos.Summary> content = base
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(comment.id.desc())
			.fetch();
		return new PageImpl<>(content, pageable, total);
	}
}
