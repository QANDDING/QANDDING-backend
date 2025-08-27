package com.qandding.domain.answer.entity;

import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.user.entity.User;
import com.qandding.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerPost extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_post_id", nullable = false)
    private QuestionPost questionPost;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false)
    private AnswerType answerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    private AnswerPost(QuestionPost questionPost, AnswerType answerType, User author, User requester, String title, String content) {
        this.questionPost = questionPost;
        this.answerType = answerType;
        this.author = author;
        this.requester = requester;
        this.title = title;
        this.content = content;
    }

    public static AnswerPost createForUser(QuestionPost questionPost, User author, String title, String content) {
        return new AnswerPost(questionPost, AnswerType.USER, author, null, title, content);
    }

    public static AnswerPost createForAi(QuestionPost questionPost, User requester, String title, String content) {
        return new AnswerPost(questionPost, AnswerType.AI, null, requester, title, content);
    }
    
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
