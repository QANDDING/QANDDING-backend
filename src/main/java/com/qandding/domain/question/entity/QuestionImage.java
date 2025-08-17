package com.qandding.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionImage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "question_image_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_post_id", nullable = false)
	private QuestionPost questionPost;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(name = "sort_order")
	private Integer sortOrder;

	public QuestionImage(QuestionPost questionPost, String url, Integer sortOrder) {
		this.questionPost = questionPost;
		this.url = url;
		this.sortOrder = sortOrder;
	}
}
