package com.qandding.professor.service;

import com.qandding.professor.domain.Professor;
import com.qandding.professor.repository.ProfessorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProfessorService {
	private final ProfessorRepository professorRepository;

	public ProfessorService(ProfessorRepository professorRepository) {
		this.professorRepository = professorRepository;
	}

	public Page<Professor> list(Pageable pageable) {
		return professorRepository.findAll(pageable);
	}
}



