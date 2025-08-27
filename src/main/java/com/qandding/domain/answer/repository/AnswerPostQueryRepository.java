package com.qandding.domain.answer.repository;

import static com.qandding.domain.answer.entity.QAnswerPost.answerPost;
import static com.qandding.domain.user.entity.QUser.user;

import com.qandding.domain.answer.dto.AnswerPostDtos;
import com.qandding.domain.answer.entity.AnswerType;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnswerPostQueryRepository {
	private final JPAQueryFactory query;

	public AnswerPostQueryRepository(JPAQueryFactory query) {
		this.query = query;
	}

	public Page<AnswerPostDtos.Summary> findSummaries(Long questionPostId, Pageable pageable) {
		// 1. 전체 개수 조회
		long total = query
			.from(answerPost)
			.join(answerPost.author, user)
			.where(answerPost.questionPost.id.eq(questionPostId)
				.and(answerPost.answerType.eq(AnswerType.USER))) // Filter by AnswerType.USER
			.fetchCount();

		// 2. 페이징된 데이터 조회
		List<AnswerPostDtos.Summary> content = query
			.select(Projections.constructor(AnswerPostDtos.Summary.class,
				answerPost.id,
				answerPost.title,
				user.nickname,
				answerPost.createdAt
			))
			.from(answerPost)
			.join(answerPost.author, user)
			.where(answerPost.questionPost.id.eq(questionPostId)
				.and(answerPost.answerType.eq(AnswerType.USER))) // Filter by AnswerType.USER
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(answerPost.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}

	// 유저별 답변 조회
	public Page<AnswerPostDtos.Summary> findSummariesByUserId(Long userId, Pageable pageable) {
		// 1. 전체 개수 조회
		long total = query
			.from(answerPost)
			.join(answerPost.author, user)
			.where(answerPost.author.id.eq(userId)
				.and(answerPost.answerType.eq(AnswerType.USER))) // Filter by AnswerType.USER
			.fetchCount();

		// 2. 페이징된 데이터 조회
		List<AnswerPostDtos.Summary> content = query
			.select(Projections.constructor(AnswerPostDtos.Summary.class,
				answerPost.id,
				answerPost.title,
				user.nickname,
				answerPost.createdAt
			))
			.from(answerPost)
			.join(answerPost.author, user)
			.where(answerPost.author.id.eq(userId)
				.and(answerPost.answerType.eq(AnswerType.USER))) // Filter by AnswerType.USER
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(answerPost.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}

	// 유저별 + 질문별 답변 조회
	public Page<AnswerPostDtos.Summary> findSummariesByUserIdAndQuestionId(Long userId, Long questionPostId, Pageable pageable) {
		// 1. 전체 개수 조회
		long total = query
			.from(answerPost)
			.join(answerPost.author, user)
			.where(answerPost.author.id.eq(userId)
				.and(answerPost.questionPost.id.eq(questionPostId))
				.and(answerPost.answerType.eq(AnswerType.USER))) // Filter by AnswerType.USER
			.fetchCount();

		// 2. 페이징된 데이터 조회
		List<AnswerPostDtos.Summary> content = query
			.select(Projections.constructor(AnswerPostDtos.Summary.class,
				answerPost.id,
				answerPost.title,
				user.nickname,
				answerPost.createdAt
			))
			.from(answerPost)
			.join(answerPost.author, user)
			.where(answerPost.author.id.eq(userId)
				.and(answerPost.questionPost.id.eq(questionPostId))
				.and(answerPost.answerType.eq(AnswerType.USER))) // Filter by AnswerType.USER
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(answerPost.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}
}
