package com.qandding.domain.subject.repository;

import com.qandding.domain.subject.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Page<Subject> findByNameContainingIgnoreCase(String query, Pageable pageable);
}
