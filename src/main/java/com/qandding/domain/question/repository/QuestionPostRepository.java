package com.qandding.domain.question.repository;

import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.subject.entity.Subject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionPostRepository extends JpaRepository<QuestionPost, Long> {
	@EntityGraph(attributePaths = {"user", "subject", "professor"})
	Page<QuestionPost> findBySubject(Subject subject, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "subject", "professor"})
	Page<QuestionPost> findByProfessor(Professor professor, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "subject", "professor"})
	Page<QuestionPost> findAll(Pageable pageable);
}
