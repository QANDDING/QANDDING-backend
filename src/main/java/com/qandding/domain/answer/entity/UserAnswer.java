package com.qandding.domain.answer.entity;

import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.user.entity.User;
import com.qandding.global.common.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAnswer extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_answer_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_post_id", nullable = false)
	private QuestionPost questionPost;

	@Column(nullable = false, length = 200)
	private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

	public UserAnswer(User user, QuestionPost questionPost, String title, String content) {
		this.user = user;
		this.questionPost = questionPost;
		this.title = title;
		this.content = content;
	}
}
