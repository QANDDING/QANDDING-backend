package com.qandding.domain.answer.repository;

import static com.qandding.domain.answer.entity.QAnswerPost.answerPost;
import static com.qandding.domain.user.entity.QUser.user;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.question.entity.QQuestionPost;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnswerQueryRepository {
	private final JPAQueryFactory query;

	public AnswerQueryRepository(JPAQueryFactory query) {
		this.query = query;
	}

	public Page<AnswerDtos.Summary> findSummaries(Long questionPostId, Pageable pageable) {
		QQuestionPost q = QQuestionPost.questionPost;
		var base = query
			.select(Projections.constructor(AnswerDtos.Summary.class,
				answerPost.id,
				answerPost.title,
				user.nickname,
				answerPost.aiAnswer.isNotNull(),
				answerPost.createdAt
			))
			.from(answerPost)
			.join(answerPost.user, user)
			.join(answerPost.questionPost, q)
			.where(q.id.eq(questionPostId));

		long total = base.fetch().size();
		List<AnswerDtos.Summary> content = base
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(answerPost.id.desc())
			.fetch();
		return new PageImpl<>(content, pageable, total);
	}
}
