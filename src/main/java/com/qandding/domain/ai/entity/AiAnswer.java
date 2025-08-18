package com.qandding.domain.ai.entity;

import com.qandding.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnswer extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ai_answer_id")
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String content;

	public AiAnswer(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}
}
