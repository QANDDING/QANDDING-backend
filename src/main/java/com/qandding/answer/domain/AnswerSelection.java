package com.qandding.answer.domain;

import com.qandding.question.domain.QuestionPost;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_selection", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"question_post_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerSelection {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "answer_selection_id")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_post_id", nullable = false)
	private QuestionPost questionPost;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "answer_post_id", nullable = false)
	private AnswerPost answerPost;

	public AnswerSelection(QuestionPost questionPost, AnswerPost answerPost) {
		this.questionPost = questionPost;
		this.answerPost = answerPost;
	}
}
