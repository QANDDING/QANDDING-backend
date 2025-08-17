package com.qandding.domain.answer.entity;

import com.qandding.domain.ai.entity.AiAnswer;
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
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_post_id", nullable = false)
	private QuestionPost questionPost;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "ai_answer_id")
	private AiAnswer aiAnswer;

	@Column(nullable = false, length = 200)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	public AnswerPost(User user, QuestionPost questionPost, AiAnswer aiAnswer, String title, String content) {
		this.user = user;
		this.questionPost = questionPost;
		this.aiAnswer = aiAnswer;
		this.title = title;
		this.content = content;
	}

	public AnswerPost(User user, QuestionPost questionPost, String title, String content) {
		this.user = user;
		this.questionPost = questionPost;
		this.aiAnswer = null;
		this.title = title;
		this.content = content;
	}
}
