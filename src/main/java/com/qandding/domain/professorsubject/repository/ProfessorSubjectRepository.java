package com.qandding.domain.professorsubject.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qandding.domain.professorsubject.entity.ProfessorSubject;
import com.qandding.domain.professorsubject.entity.ProfessorSubjectId;

public interface ProfessorSubjectRepository extends JpaRepository<ProfessorSubject, ProfessorSubjectId> {
}
