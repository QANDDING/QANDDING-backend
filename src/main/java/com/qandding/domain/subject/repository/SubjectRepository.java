package com.qandding.domain.subject.repository;

import com.qandding.domain.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByNameContainingIgnoreCase(String query);
}

