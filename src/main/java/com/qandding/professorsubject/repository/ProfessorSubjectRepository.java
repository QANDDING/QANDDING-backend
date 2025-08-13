package com.qandding.professorsubject.repository;

import com.qandding.professorsubject.domain.ProfessorSubject;
import com.qandding.professorsubject.domain.ProfessorSubjectId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorSubjectRepository extends JpaRepository<ProfessorSubject, ProfessorSubjectId> {
}
