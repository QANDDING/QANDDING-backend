package com.qandding.question.service;

import com.qandding.common.error.BusinessException;
import com.qandding.common.error.ErrorCode;
import com.qandding.professor.repository.ProfessorRepository;
import com.qandding.question.domain.QuestionImage;
import com.qandding.question.domain.QuestionPost;
import com.qandding.question.presentation.dto.QuestionDtos;
import com.qandding.question.repository.QuestionImageRepository;
import com.qandding.question.repository.QuestionPostRepository;
import com.qandding.question.repository.QuestionQueryRepository;
import com.qandding.subject.repository.SubjectRepository;
import com.qandding.user.domain.User;
import com.qandding.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class QuestionService {
	private final QuestionPostRepository questionPostRepository;
	private final QuestionQueryRepository questionQueryRepository;
	private final QuestionImageRepository questionImageRepository;
	private final UserRepository userRepository;
	private final SubjectRepository subjectRepository;
	private final ProfessorRepository professorRepository;

	public QuestionService(QuestionPostRepository questionPostRepository,
	                      QuestionQueryRepository questionQueryRepository,
	                      QuestionImageRepository questionImageRepository,
	                      UserRepository userRepository,
	                      SubjectRepository subjectRepository,
	                      ProfessorRepository professorRepository) {
		this.questionPostRepository = questionPostRepository;
		this.questionQueryRepository = questionQueryRepository;
		this.questionImageRepository = questionImageRepository;
		this.userRepository = userRepository;
		this.subjectRepository = subjectRepository;
		this.professorRepository = professorRepository;
	}

	@Transactional
	public Long createQuestion(Long userId,
	                          Long subjectId,
	                          Long professorId,
	                          String title,
	                          String content,
	                          List<String> imageUrls) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		var subject = subjectRepository.findById(subjectId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
		var professor = professorRepository.findById(professorId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
		QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));
		if (imageUrls != null) {
			int i = 0;
			for (String url : imageUrls) {
				questionImageRepository.save(new QuestionImage(post, url, i++));
			}
		}
		return post.getId();
	}

	public Page<QuestionDtos.Summary> findQuestions(Long subjectId, Long professorId, Pageable pageable) {
		return questionQueryRepository.findSummaries(subjectId, professorId, pageable);
	}

	public QuestionDtos.Detail getQuestionDetail(Long id) {
		QuestionPost q = questionPostRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		List<String> images = questionImageRepository
			.findByQuestionPostIdOrderBySortOrderAsc(id)
			.stream().map(QuestionImage::getUrl).collect(Collectors.toList());
		return new QuestionDtos.Detail(
				q.getId(), q.getTitle(), q.getContent(), q.getUser().getNickname(),
				q.getSubject().getName(), q.getProfessor().getName(), q.getCreatedAt(), images);
	}

	@Transactional
	public void deleteQuestion(Long userId, Long id) {
		QuestionPost post = questionPostRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		if (!post.getUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		questionPostRepository.delete(post);
	}

	public boolean matchesSubject(Long id, Long subjectId) {
		QuestionPost post = questionPostRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		return post.getSubject().getId().equals(subjectId);
	}

	public boolean matchesProfessor(Long id, Long professorId) {
		QuestionPost post = questionPostRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		return post.getProfessor().getId().equals(professorId);
	}
}

