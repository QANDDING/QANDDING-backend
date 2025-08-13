package com.qandding.question.repository;

import static com.qandding.question.domain.QQuestionPost.questionPost;
import static com.qandding.user.domain.QUser.user;
import static com.qandding.subject.domain.QSubject.subject;
import static com.qandding.professor.domain.QProfessor.professor;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.qandding.question.presentation.dto.QuestionDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionQueryRepository {
	private final JPAQueryFactory query;

	public QuestionQueryRepository(JPAQueryFactory query) {
		this.query = query;
	}

	public Page<QuestionDtos.Summary> findSummaries(Long subjectId, Long professorId, Pageable pageable) {
		var base = query
			.select(Projections.constructor(QuestionDtos.Summary.class,
				questionPost.id,
				questionPost.title,
				user.nickname,
				subject.name,
				professor.name,
				questionPost.createdAt
			))
			.from(questionPost)
			.join(questionPost.user, user)
			.join(questionPost.subject, subject)
			.join(questionPost.professor, professor);

		if (subjectId != null) base.where(subject.id.eq(subjectId));
		if (professorId != null) base.where(professor.id.eq(professorId));

		long total = base.fetch().size();
		List<QuestionDtos.Summary> content = base
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(questionPost.id.desc())
			.fetch();
		return new PageImpl<>(content, pageable, total);
	}
}
