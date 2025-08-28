package com.qandding.domain.question.entity;

import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.subject.entity.Subject;
import com.qandding.domain.user.entity.User;
import com.qandding.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "question_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionPost extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "question_post_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "professor_id", nullable = false)
	private Professor professor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@Column(nullable = false, length = 200)
	private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

	public QuestionPost(User user, Professor professor, Subject subject, String title, String content) {
		this.user = user;
		this.professor = professor;
		this.subject = subject;
		this.title = title;
		this.content = content;
	}

	public void updateTitle(String title) {
		if (title != null && title.length() > 200) {
			this.title = title.substring(0, 200);
		} else if (title != null) {
			this.title = title;
		}
	}

	public void updateContent(String content) {
		if (content != null) {
			this.content = content;
		}
	}

	public void updateSubject(Subject subject) {
		if (subject != null) {
			this.subject = subject;
		}
	}

	public void updateProfessor(Professor professor) {
		if (professor != null) {
			this.professor = professor;
		}
	}
}
