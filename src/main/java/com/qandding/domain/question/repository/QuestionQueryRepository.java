package com.qandding.domain.question.repository;

import static com.qandding.domain.answer.entity.QAnswerSelection.answerSelection;
import static com.qandding.domain.professor.entity.QProfessor.professor;
import static com.qandding.domain.question.entity.QQuestionPost.questionPost;
import static com.qandding.domain.subject.entity.QSubject.subject;
import static com.qandding.domain.user.entity.QUser.user;

import com.qandding.domain.question.dto.QuestionDtos;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class QuestionQueryRepository {
	private final JPAQueryFactory query;

	public QuestionQueryRepository(JPAQueryFactory query) {
		this.query = query;
	}

	public Page<QuestionDtos.Summary> findSummaries(Long subjectId, Long professorId, Pageable pageable) {
		log.debug("findSummaries() 호출됨 - subjectId: {}, professorId: {}, pageable: {}", subjectId, professorId, pageable);
		// 1. 전체 개수 조회 (별도 쿼리)
		long total = query
			.from(questionPost)
			.where(subjectId != null ? questionPost.subject.id.eq(subjectId) : null,
				   professorId != null ? questionPost.professor.id.eq(professorId) : null)
			.fetchCount();
		log.debug("전체 개수 조회 결과: {}", total);

		// 2. 페이징된 데이터 조회 (완전히 별도 쿼리)
		var selectQuery = query
			.select(Projections.constructor(QuestionDtos.Summary.class,
				questionPost.id,
				questionPost.title,
				user.nickname,
				subject.name,
				professor.name,
				questionPost.createdAt,
				JPAExpressions.selectOne().from(answerSelection).where(answerSelection.questionPost.eq(questionPost)).exists()
			))
			.from(questionPost)
			.leftJoin(questionPost.user, user)
			.leftJoin(questionPost.subject, subject)
			.leftJoin(questionPost.professor, professor);

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
			log.debug("정렬 적용 - property: {}, direction: {}", property, order.getDirection());
			
			// 유효한 정렬 필드인지 확인하고 적용
			if ("id".equals(property)) {
				selectQuery.orderBy(order.isAscending() ? questionPost.id.asc() : questionPost.id.desc());
			} else if ("title".equals(property)) {
				selectQuery.orderBy(order.isAscending() ? questionPost.title.asc() : questionPost.title.desc());
			} else if ("createdAt".equals(property)) {
				selectQuery.orderBy(order.isAscending() ? questionPost.createdAt.asc() : questionPost.createdAt.desc());
			} else {
				// 기본 정렬: id 내림차순
				log.debug("지원하지 않는 정렬 필드 '{}'이므로 기본 정렬(id desc)을 적용합니다.", property);
				selectQuery.orderBy(questionPost.id.desc());
			}
		} else {
			// 정렬이 없으면 기본값: id 내림차순
			log.debug("정렬 조건이 없으므로 기본 정렬(id desc)을 적용합니다.");
			selectQuery.orderBy(questionPost.id.desc());
		}

		List<QuestionDtos.Summary> content = selectQuery.fetch();
		log.debug("조회된 데이터 {}건", content.size());

		Page<QuestionDtos.Summary> pageResult = new PageImpl<>(content, pageable, total);
		log.debug("findSummaries() 반환 - Page: {}/{} (총 {}개)", pageResult.getNumber(), pageResult.getTotalPages(), pageResult.getTotalElements());
		return pageResult;
	}

	public QuestionDtos.Detail findDetailById(Long id) {
		log.debug("findDetailById() 호출됨 - id: {}", id);
		QuestionDtos.Detail result = query
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
		log.debug("findDetailById() 반환 - result: {}", result != null ? "found" : "not found");
		return result;
	}
}
