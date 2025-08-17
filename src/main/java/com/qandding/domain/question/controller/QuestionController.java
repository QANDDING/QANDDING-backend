package com.qandding.domain.question.controller;

import com.qandding.global.common.paging.PageResponse;
import com.qandding.domain.professor.repository.ProfessorRepository;
import com.qandding.domain.question.dto.QuestionDtos;
import com.qandding.domain.question.entity.QuestionImage;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionImageRepository;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.question.repository.QuestionQueryRepository;
import com.qandding.domain.subject.repository.SubjectRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.domain.user.entity.CustomUserPrincipal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
	private final QuestionPostRepository questionPostRepository;
	private final QuestionQueryRepository questionQueryRepository;
	private final QuestionImageRepository questionImageRepository;
	private final UserRepository userRepository;
	private final SubjectRepository subjectRepository;
	private final ProfessorRepository professorRepository;

	public QuestionController(QuestionPostRepository questionPostRepository,
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

	public record CreateQuestionRequest(
			@NotBlank String title,
			@NotBlank String content,
			Long subjectId,
			Long professorId,
			List<String> imageUrls
	) {}

	@PostMapping
	@Transactional
	public ResponseEntity<Long> create(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @RequestBody CreateQuestionRequest req) {
		User user = userRepository.findById(principal.getUserId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		var subject = subjectRepository.findById(req.subjectId()).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
		var professor = professorRepository.findById(req.professorId()).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
		QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, req.title(), req.content()));
		if (req.imageUrls() != null) {
			int i = 0;
			for (String url : req.imageUrls()) {
				questionImageRepository.save(new QuestionImage(post, url, i++));
			}
		}
		return ResponseEntity.ok(post.getId());
	}

	@GetMapping
	public ResponseEntity<PageResponse<QuestionDtos.Summary>> list(
			@RequestParam(required = false) Long subjectId,
			@RequestParam(required = false) Long professorId,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		var page = questionQueryRepository.findSummaries(subjectId, professorId, pageable);
		return ResponseEntity.ok(PageResponse.of(page));
	}

	@GetMapping("/{id}")
	public ResponseEntity<QuestionDtos.Detail> get(@PathVariable Long id) {
		QuestionPost q = questionPostRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		List<String> images = questionImageRepository
			.findByQuestionPostIdOrderBySortOrderAsc(id)
			.stream().map(QuestionImage::getUrl).collect(Collectors.toList());
		QuestionDtos.Detail dto = new QuestionDtos.Detail(
				q.getId(), q.getTitle(), q.getContent(), q.getUser().getNickname(),
				q.getSubject().getName(), q.getProfessor().getName(), q.getCreatedAt(), images);
		return ResponseEntity.ok(dto);
	}

	@DeleteMapping("/{id}")
	@Transactional
	public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                  @PathVariable Long id) {
		QuestionPost q = questionPostRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
		if (!q.getUser().getId().equals(principal.getUserId())) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		List<QuestionImage> images = questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(id);
		questionImageRepository.deleteAll(images);
		questionPostRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
