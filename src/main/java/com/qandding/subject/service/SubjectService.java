package com.qandding.subject.service;

import com.qandding.subject.domain.Subject;
import com.qandding.subject.repository.SubjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SubjectService {
	private final SubjectRepository subjectRepository;

	public SubjectService(SubjectRepository subjectRepository) {
		this.subjectRepository = subjectRepository;
	}

	public Page<Subject> list(Pageable pageable) {
		return subjectRepository.findAll(pageable);
	}
}



