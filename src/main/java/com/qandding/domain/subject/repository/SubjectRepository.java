package com.qandding.domain.subject.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qandding.domain.subject.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
}
