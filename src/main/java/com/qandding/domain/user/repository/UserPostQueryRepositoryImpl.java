package com.qandding.domain.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.question.entity.QuestionPost;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import static com.qandding.domain.question.entity.QQuestionPost.questionPost;
import static com.qandding.domain.answer.entity.QAnswerPost.answerPost;

@Repository
@RequiredArgsConstructor
public class UserPostQueryRepositoryImpl implements UserPostQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<QuestionPost> findQuestionPostsByUserId(Long userId, Pageable pageable) {
        var query = queryFactory
                .selectFrom(questionPost)
                .where(questionPost.user.id.eq(userId))
                .orderBy(questionPost.createdAt.desc());

        var content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var total = queryFactory
                .selectFrom(questionPost)
                .where(questionPost.user.id.eq(userId))
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<AnswerPost> findAnswerPostsByUserId(Long userId, Pageable pageable) {
        var query = queryFactory
                .selectFrom(answerPost)
                .where(answerPost.user.id.eq(userId))
                .orderBy(answerPost.createdAt.desc());

        var content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var total = queryFactory
                .selectFrom(answerPost)
                .where(answerPost.user.id.eq(userId))
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
    }
}
