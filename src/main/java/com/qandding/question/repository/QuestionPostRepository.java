package com.qandding.question.repository;

import com.qandding.question.domain.QuestionPost;
import com.qandding.subject.domain.Subject;
import com.qandding.professor.domain.Professor;
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
