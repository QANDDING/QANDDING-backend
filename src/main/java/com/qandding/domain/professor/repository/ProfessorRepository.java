package com.qandding.domain.professor.repository;

import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.professorsubject.entity.ProfessorSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    @Query("select p from Professor p join com.qandding.domain.professorsubject.entity.ProfessorSubject ps on ps.professor = p where ps.subject.id = :subjectId")
    Page<Professor> findBySubjectId(@Param("subjectId") Long subjectId, Pageable pageable);
}
