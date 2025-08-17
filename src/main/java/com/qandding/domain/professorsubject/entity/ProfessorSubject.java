package com.qandding.domain.professorsubject.entity;

import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.subject.entity.Subject;
import com.qandding.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "professor_subject")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfessorSubject extends BaseTimeEntity {
	@EmbeddedId
	private ProfessorSubjectId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("professorId")
	@JoinColumn(name = "professor_id", nullable = false)
	private Professor professor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("subjectId")
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	public ProfessorSubject(Professor professor, Subject subject) {
		this.professor = professor;
		this.subject = subject;
		this.id = new ProfessorSubjectId(professor.getId(), subject.getId());
	}
}
