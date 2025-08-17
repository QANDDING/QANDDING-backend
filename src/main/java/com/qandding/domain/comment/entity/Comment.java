package com.qandding.domain.comment.entity;

import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.user.entity.User;
import com.qandding.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "answer_post_id", nullable = false)
	private AnswerPost answerPost;

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "ai_answer_id")
	private AiAnswer aiAnswer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String content;

	public Comment(AnswerPost answerPost, User user, String content) {
		this.answerPost = answerPost;
		this.aiAnswer = null;
		this.user = user;
		this.content = content;
	}

	public Comment(AnswerPost answerPost, AiAnswer aiAnswer, User user, String content) {
		this.answerPost = answerPost;
		this.aiAnswer = aiAnswer;
		this.user = user;
		this.content = content;
	}
}
