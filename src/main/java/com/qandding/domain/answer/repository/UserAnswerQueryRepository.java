package com.qandding.domain.answer.repository;

import static com.qandding.domain.answer.entity.QUserAnswer.userAnswer;
import static com.qandding.domain.user.entity.QUser.user;

import com.qandding.domain.answer.dto.UserAnswerDtos;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserAnswerQueryRepository {
	private final JPAQueryFactory query;

	public UserAnswerQueryRepository(JPAQueryFactory query) {
		this.query = query;
	}

	public Page<UserAnswerDtos.Summary> findSummaries(Long questionPostId, Pageable pageable) {
		// 1. 전체 개수 조회
		long total = query
			.from(userAnswer)
			.join(userAnswer.user, user)
			.where(userAnswer.questionPost.id.eq(questionPostId))
			.fetchCount();

		// 2. 페이징된 데이터 조회
		List<UserAnswerDtos.Summary> content = query
			.select(Projections.constructor(UserAnswerDtos.Summary.class,
				userAnswer.id,
				userAnswer.title,
				user.nickname,
				userAnswer.createdAt
			))
			.from(userAnswer)
			.join(userAnswer.user, user)
			.where(userAnswer.questionPost.id.eq(questionPostId))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(userAnswer.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}

	// 유저별 답변 조회
	public Page<UserAnswerDtos.Summary> findSummariesByUserId(Long userId, Pageable pageable) {
		// 1. 전체 개수 조회
		long total = query
			.from(userAnswer)
			.join(userAnswer.user, user)
			.where(userAnswer.user.id.eq(userId))
			.fetchCount();

		// 2. 페이징된 데이터 조회
		List<UserAnswerDtos.Summary> content = query
			.select(Projections.constructor(UserAnswerDtos.Summary.class,
				userAnswer.id,
				userAnswer.title,
				user.nickname,
				userAnswer.createdAt
			))
			.from(userAnswer)
			.join(userAnswer.user, user)
			.where(userAnswer.user.id.eq(userId))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(userAnswer.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}

	// 유저별 + 질문별 답변 조회
	public Page<UserAnswerDtos.Summary> findSummariesByUserIdAndQuestionId(Long userId, Long questionPostId, Pageable pageable) {
		// 1. 전체 개수 조회
		long total = query
			.from(userAnswer)
			.join(userAnswer.user, user)
			.where(userAnswer.user.id.eq(userId)
				.and(userAnswer.questionPost.id.eq(questionPostId)))
			.fetchCount();

		// 2. 페이징된 데이터 조회
		List<UserAnswerDtos.Summary> content = query
			.select(Projections.constructor(UserAnswerDtos.Summary.class,
				userAnswer.id,
				userAnswer.title,
				user.nickname,
				userAnswer.createdAt
			))
			.from(userAnswer)
			.join(userAnswer.user, user)
			.where(userAnswer.user.id.eq(userId)
				.and(userAnswer.questionPost.id.eq(questionPostId)))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(userAnswer.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}
}
