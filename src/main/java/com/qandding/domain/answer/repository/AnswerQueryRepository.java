package com.qandding.domain.answer.repository;

import static com.qandding.domain.answer.entity.QAnswerPost.answerPost;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.entity.AnswerType;
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
		
		long total = query
			.select(answerPost.count())
			.from(answerPost)
			.join(answerPost.questionPost, q)
			.where(q.id.eq(questionPostId))
			.fetchOne();

		List<AnswerDtos.Summary> base = query
			.select(Projections.constructor(AnswerDtos.Summary.class,
				answerPost.id,
				answerPost.title,
                new CaseBuilder()
                    .when(answerPost.author.isNotNull()).then(answerPost.author.nickname)
                    .when(answerPost.requester.isNotNull()).then(answerPost.requester.nickname)
                    .otherwise("AI"),
				answerPost.answerType.eq(AnswerType.AI),
				answerPost.createdAt
			))
			.from(answerPost)
            .leftJoin(answerPost.author)
            .leftJoin(answerPost.requester)
			.join(answerPost.questionPost, q)
			.where(q.id.eq(questionPostId))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(answerPost.createdAt.desc())
			.fetch();

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