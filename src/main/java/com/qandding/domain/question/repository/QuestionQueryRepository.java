package com.qandding.domain.question.repository;

import static com.qandding.domain.professor.entity.QProfessor.professor;
import static com.qandding.domain.question.entity.QQuestionPost.questionPost;
import static com.qandding.domain.subject.entity.QSubject.subject;
import static com.qandding.domain.answer.entity.QAnswerSelection.answerSelection;
import static com.qandding.domain.user.entity.QUser.user;

import com.qandding.domain.question.dto.QuestionDtos;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionQueryRepository {
	private final JPAQueryFactory query;

	public QuestionQueryRepository(JPAQueryFactory query) {
		this.query = query;
	}

	public Page<QuestionDtos.Summary> findSummaries(Long subjectId, Long professorId, Pageable pageable) {
		// 1. 전체 개수 조회 (별도 쿼리)
		long total = query
			.from(questionPost)
			.where(subjectId != null ? questionPost.subject.id.eq(subjectId) : null,
				   professorId != null ? questionPost.professor.id.eq(professorId) : null)
			.fetchCount();

		// 2. 페이징된 데이터 조회 (완전히 별도 쿼리)
        var selectQuery = query
            .select(Projections.constructor(QuestionDtos.Summary.class,
                questionPost.id,
                questionPost.title,
                user.nickname,
                subject.name,
                professor.name,
                questionPost.createdAt,
                answerSelection.id.isNotNull()
            ))
            .from(questionPost)
            .leftJoin(questionPost.user, user)
            .leftJoin(questionPost.subject, subject)
            .leftJoin(questionPost.professor, professor)
            .leftJoin(answerSelection).on(answerSelection.questionPost.id.eq(questionPost.id));

		// where 조건 추가
		if (subjectId != null) {
			selectQuery.where(subject.id.eq(subjectId));
		}
		if (professorId != null) {
			selectQuery.where(professor.id.eq(professorId));
		}

		// 페이징 적용
		selectQuery.offset(pageable.getOffset())
			.limit(pageable.getPageSize());

		// 정렬 처리
		if (pageable.getSort().isSorted()) {
			Sort.Order order = pageable.getSort().iterator().next();
			String property = order.getProperty();
			
			// 유효한 정렬 필드인지 확인하고 적용
			if ("id".equals(property)) {
				selectQuery.orderBy(order.isAscending() ? questionPost.id.asc() : questionPost.id.desc());
			} else if ("title".equals(property)) {
				selectQuery.orderBy(order.isAscending() ? questionPost.title.asc() : questionPost.title.desc());
			} else if ("createdAt".equals(property)) {
				selectQuery.orderBy(order.isAscending() ? questionPost.createdAt.asc() : questionPost.createdAt.desc());
			} else {
				// 기본 정렬: id 내림차순
				selectQuery.orderBy(questionPost.id.desc());
			}
		} else {
			// 정렬이 없으면 기본값: id 내림차순
			selectQuery.orderBy(questionPost.id.desc());
		}

		List<QuestionDtos.Summary> content = selectQuery.fetch();

		return new PageImpl<>(content, pageable, total);
	}

	public QuestionDtos.Detail findDetailById(Long id) {
		return query
			.select(Projections.constructor(QuestionDtos.Detail.class,
				questionPost.id,
				questionPost.title,
				questionPost.content,
				user.nickname,
				subject.name,
				professor.name,
				questionPost.createdAt
			))
			.from(questionPost)
			.leftJoin(questionPost.user, user)
			.leftJoin(questionPost.subject, subject)
			.leftJoin(questionPost.professor, professor)
			.where(questionPost.id.eq(id))
			.fetchOne();
	}
}
