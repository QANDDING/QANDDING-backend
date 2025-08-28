package com.qandding.domain.question.repository;

import static com.qandding.domain.answer.entity.QAnswerPost.answerPost;
import static com.qandding.domain.answer.entity.QAnswerSelection.answerSelection;
import static com.qandding.domain.professor.entity.QProfessor.professor;
import static com.qandding.domain.question.entity.QQuestionPost.questionPost;
import static com.qandding.domain.subject.entity.QSubject.subject;
import static com.qandding.domain.user.entity.QUser.user;

import com.qandding.domain.answer.entity.AnswerType;

import com.qandding.domain.question.dto.QuestionDtos;
import com.qandding.domain.question.dto.QuestionDtos.QuestionDetail;
import com.qandding.domain.question.dto.QuestionStatusFilter;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class QuestionQueryRepository {
    private final JPAQueryFactory query;

    public QuestionQueryRepository(JPAQueryFactory query) {
        this.query = query;
    }

    public Page<QuestionDtos.Summary> findSummaries(Long subjectId, Long professorId, String keyword, QuestionStatusFilter status, Pageable pageable) {
        log.debug("findSummaries() 호출됨 - subjectId: {}, professorId: {}, keyword: {}, status: {}, pageable: {}", subjectId, professorId, keyword, status, pageable);

        List<BooleanExpression> conditions = new ArrayList<>();
        if (subjectId != null) {
            conditions.add(questionPost.subject.id.eq(subjectId));
        }
        if (professorId != null) {
            conditions.add(questionPost.professor.id.eq(professorId));
        }

        if (keyword != null && !keyword.isBlank()) {
            BooleanExpression titleContains = questionPost.title.like("%" + keyword + "%");
            BooleanExpression contentContains = questionPost.content.like("%" + keyword + "%");
            conditions.add(titleContains.or(contentContains));
        }

        if (status != null) {
            switch (status) {
                case ANSWERED:
                    BooleanExpression hasAiAnswer = JPAExpressions.selectOne().from(answerPost).where(answerPost.questionPost.eq(questionPost).and(answerPost.answerType.eq(AnswerType.AI))).exists();
                    BooleanExpression hasMemberAnswer = JPAExpressions.selectOne().from(answerPost).where(answerPost.questionPost.eq(questionPost).and(answerPost.answerType.eq(AnswerType.USER))).exists();
                    conditions.add(hasAiAnswer.or(hasMemberAnswer));
                    break;
                case UNANSWERED:
                    conditions.add(JPAExpressions.selectOne().from(answerPost).where(answerPost.questionPost.eq(questionPost).and(answerPost.answerType.eq(AnswerType.USER))).notExists());
                    break;
                case ADOPTED:
                    conditions.add(JPAExpressions.selectOne().from(answerSelection).where(answerSelection.questionPost.eq(questionPost)).exists());
                    break;
                case ALL:
                default:
                    // No additional conditions
                    break;
            }
        }

        // 1. 전체 개수 조회 (필터링 적용)
        long total = query
                .select(questionPost.id.count())
                .from(questionPost)
                .where(conditions.toArray(new BooleanExpression[0]))
                .fetchOne();
        log.debug("필터링된 전체 개수 조회 결과: {}", total);

        // 2. 페이징된 데이터 조회 (완전히 별도 쿼리)
        var selectQuery = query
                .select(Projections.constructor(QuestionDtos.Summary.class,
                        questionPost.id,
                        questionPost.title,
                        user.nickname,
                        subject.name,
                        professor.name,
                        questionPost.createdAt,
                        JPAExpressions.selectOne().from(answerPost).where(answerPost.questionPost.eq(questionPost).and(answerPost.answerType.eq(AnswerType.AI))).exists(), // hasAiAnswer
                        JPAExpressions.selectOne().from(answerPost).where(answerPost.questionPost.eq(questionPost).and(answerPost.answerType.eq(AnswerType.USER))).exists(),    // hasMemberAnswer
                        JPAExpressions.selectOne().from(answerSelection).where(answerSelection.questionPost.eq(questionPost)).exists() // isAdopted
                ))
                .from(questionPost)
                .leftJoin(questionPost.user, user)
                .leftJoin(questionPost.subject, subject)
                .leftJoin(questionPost.professor, professor)
                .where(conditions.toArray(new BooleanExpression[0]));

        // 페이징 적용
        selectQuery.offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // 정렬 처리 (기본값: 최신순)
        selectQuery.orderBy(questionPost.id.desc());

        List<QuestionDtos.Summary> content = total > 0 ? selectQuery.fetch() : new ArrayList<>();
        log.debug("조회된 데이터 {}건", content.size());

        Page<QuestionDtos.Summary> pageResult = new PageImpl<>(content, pageable, total);
        log.debug("findSummaries() 반환 - Page: {}/{} (총 {}개)", pageResult.getNumber(), pageResult.getTotalPages(), pageResult.getTotalElements());
        return pageResult;
    }

    public QuestionDetail findDetailById(Long id) {
        log.debug("findDetailById() 호출됨 - id: {}", id);
        QuestionDetail result = query
                .select(Projections.constructor(QuestionDetail.class,
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
