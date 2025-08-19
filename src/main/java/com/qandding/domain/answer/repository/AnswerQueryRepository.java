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

import com.qandding.global.storage.S3PresignService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class AnswerQueryRepository {
	private final JPAQueryFactory query;
	private final AnswerImageRepository answerImageRepository;
    private final S3PresignService s3PresignService;

	public AnswerQueryRepository(JPAQueryFactory query, AnswerImageRepository answerImageRepository, S3PresignService s3PresignService) {
		this.query = query;
		this.answerImageRepository = answerImageRepository;
        this.s3PresignService = s3PresignService;
	}

	public Page<AnswerDtos.Summary> findSummaries(Long questionPostId, Pageable pageable) {
		QQuestionPost q = QQuestionPost.questionPost;
		
		// 1. 전체 개수 조회 (효율적으로)
		long total = query
			.select(answerPost.count())
			.from(answerPost)
			.join(answerPost.questionPost, q)
			.where(q.id.eq(questionPostId))
			.fetchOne();

		// 2. 기본 필드 페이징 조회
		List<AnswerDtos.Summary> base = query
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
			.where(q.id.eq(questionPostId))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(answerPost.createdAt.desc())
			.fetch();

		// 3. 이미지 일괄 조회 후 매핑
		List<Long> ids = base.stream().map(AnswerDtos.Summary::getId).toList();
		Map<Long, List<String>> imageMap = new HashMap<>();
		if (!ids.isEmpty()) {
			var images = answerImageRepository.findByAnswerPostIdInOrderBySortOrderAsc(ids);
			for (var ai : images) {
                String presigned = s3PresignService.presignGetUrlByUrl(ai.getUrl());
				imageMap.computeIfAbsent(ai.getAnswerPost().getId(), k -> new ArrayList<>()).add(presigned);
			}
		}

		List<AnswerDtos.Summary> content = base.stream()
			.map(s -> new AnswerDtos.Summary(
				s.getId(), s.getTitle(), s.getAuthorNickname(), s.isHasAi(), s.getCreatedAt(),
				imageMap.getOrDefault(s.getId(), List.of())
			))
			.collect(Collectors.toList());

		return new PageImpl<>(content, pageable, total);
	}
}
