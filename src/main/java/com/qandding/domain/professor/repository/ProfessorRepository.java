package com.qandding.domain.professor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qandding.domain.professor.entity.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}
