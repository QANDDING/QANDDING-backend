package com.qandding.answer.domain;

import com.qandding.ai.domain.AiAnswer;
import com.qandding.global.jpa.BaseTimeEntity;
import com.qandding.question.domain.QuestionPost;
import com.qandding.user.domain.User;
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

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ai_answer_id", unique = true)
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
}
