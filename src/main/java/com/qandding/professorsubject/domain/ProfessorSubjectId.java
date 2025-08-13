package com.qandding.professorsubject.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfessorSubjectId implements Serializable {
	private Long professorId;
	private Long subjectId;

	public ProfessorSubjectId(Long professorId, Long subjectId) {
		this.professorId = professorId;
		this.subjectId = subjectId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProfessorSubjectId that = (ProfessorSubjectId) o;
		return Objects.equals(professorId, that.professorId) && Objects.equals(subjectId, that.subjectId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(professorId, subjectId);
	}
}
