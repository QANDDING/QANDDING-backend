package com.qandding.domain.question.controller;

import com.qandding.domain.question.dto.QuestionDtos;
import com.qandding.domain.question.entity.QuestionImage;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionImageRepository;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.question.repository.QuestionQueryRepository;
import com.qandding.domain.subject.entity.Subject;
import com.qandding.domain.subject.repository.SubjectRepository;
import com.qandding.domain.professor.entity.Professor;
import com.qandding.domain.professor.repository.ProfessorRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.common.paging.PageResponse;
import com.qandding.global.storage.S3UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/questions")
@Tag(name = "Question", description = "질문 관련 API")
public class QuestionController {
	private final QuestionPostRepository questionPostRepository;
	private final QuestionImageRepository questionImageRepository;
	private final QuestionQueryRepository questionQueryRepository;
	private final SubjectRepository subjectRepository;
	private final ProfessorRepository professorRepository;
	private final UserRepository userRepository;
	private final S3UploadService s3UploadService;

	public QuestionController(QuestionPostRepository questionPostRepository,
	                         QuestionImageRepository questionImageRepository,
	                         QuestionQueryRepository questionQueryRepository,
	                         SubjectRepository subjectRepository,
	                         ProfessorRepository professorRepository,
	                         UserRepository userRepository,
	                         S3UploadService s3UploadService) {
		this.questionPostRepository = questionPostRepository;
		this.questionImageRepository = questionImageRepository;
		this.questionQueryRepository = questionQueryRepository;
		this.subjectRepository = subjectRepository;
		this.professorRepository = professorRepository;
		this.userRepository = userRepository;
		this.s3UploadService = s3UploadService;
	}

	public record CreateQuestionRequest(
			@NotBlank String title,
			@NotBlank String content,
			Long subjectId,
			Long professorId,
			java.util.List<String> imageUrls
	) {}

	@PostMapping
	@Transactional
	@Operation(summary = "질문 생성", description = "새로운 질문을 생성합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "질문 생성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "404", description = "과목 또는 교수를 찾을 수 없음")
	})
	public ResponseEntity<Long> create(@RequestBody CreateQuestionRequest req) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		User user = userRepository.findById(customPrincipal.getUserId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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

	@PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Transactional
	@Operation(summary = "이미지와 함께 질문 생성", description = "이미지를 첨부하여 질문을 생성합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "질문 생성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "404", description = "과목 또는 교수를 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "이미지 업로드 실패")
	})
	public ResponseEntity<Long> createWithImages(
	                                           @Parameter(description = "질문 제목", required = true) @RequestParam("title") String title,
	                                           @Parameter(description = "질문 내용", required = true) @RequestParam("content") String content,
	                                           @Parameter(description = "과목 ID", required = true) @RequestParam("subjectId") Long subjectId,
	                                           @Parameter(description = "교수 ID", required = true) @RequestParam("professorId") Long professorId,
	                                           @Parameter(description = "첨부할 이미지 파일들", required = false,
	                                               content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) 
	                                           @RequestParam(value = "images", required = false) List<MultipartFile> images) {
		log.info("Creating question with images: title={}, images count={}", title, images != null ? images.size() : 0);
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		User user = userRepository.findById(customPrincipal.getUserId()).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		var subject = subjectRepository.findById(subjectId).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
		var professor = professorRepository.findById(professorId).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
		
		QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));
		
		// 이미지가 있으면 S3에 업로드하고 DB에 저장
		if (images != null && !images.isEmpty()) {
			List<String> imageUrls = new ArrayList<>();
			
			for (MultipartFile image : images) {
				try {
					if (!image.isEmpty()) {
						String imageUrl = s3UploadService.uploadImage(image);
						imageUrls.add(imageUrl);
						log.info("Image uploaded to S3: {}", imageUrl);
					}
				} catch (IOException e) {
					log.error("Failed to upload image: {}", image.getOriginalFilename(), e);
					throw new BusinessException(ErrorCode.INTERNAL_ERROR);
				}
			}
			
			// 이미지 URL들을 DB에 저장
			for (int i = 0; i < imageUrls.size(); i++) {
				questionImageRepository.save(new QuestionImage(post, imageUrls.get(i), i));
			}
			
			log.info("Saved {} images for question {}", imageUrls.size(), post.getId());
		}
		
		return ResponseEntity.ok(post.getId());
	}

	@GetMapping
	public ResponseEntity<PageResponse<QuestionDtos.Summary>> list(
			@RequestParam(required = false) Long subjectId,
			@RequestParam(required = false) Long professorId,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		
		log.info("Fetching questions with subjectId: {}, professorId: {}, page: {}, size: {}, sort: {}", 
			subjectId, professorId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
		
		try {
			var page = questionQueryRepository.findSummaries(subjectId, professorId, pageable);
			log.info("Found {} questions, total: {}", page.getContent().size(), page.getTotalElements());
			return ResponseEntity.ok(PageResponse.of(page));
		} catch (Exception e) {
			log.error("Error fetching questions: ", e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	@GetMapping("/debug/all")
	@Transactional(readOnly = true)
	public ResponseEntity<List<Long>> getAllQuestionIds() {
		List<Long> ids = questionPostRepository.findAll().stream()
			.map(QuestionPost::getId)
			.collect(Collectors.toList());
		
		log.info("All question IDs in database: {}", ids);
		return ResponseEntity.ok(ids);
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	public ResponseEntity<QuestionDtos.Detail> get(@PathVariable Long id) {
		log.info("Fetching question with id: {}", id);
		
		QuestionPost q = questionPostRepository.findById(id)
			.orElseThrow(() -> {
				log.error("Question not found with id: {}", id);
				return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
			});
		
		log.info("Found question: id={}, title={}, author={}", q.getId(), q.getTitle(), q.getUser().getNickname());
		
		// 이미지 URL 조회
		List<String> images = questionImageRepository
			.findByQuestionPostIdOrderBySortOrderAsc(id)
			.stream().map(QuestionImage::getUrl).collect(Collectors.toList());
		
		log.info("Found {} images for question {}", images.size(), id);
		
		QuestionDtos.Detail dto = new QuestionDtos.Detail(
			q.getId(), q.getTitle(), q.getContent(), 
			q.getUser().getNickname(), q.getSubject().getName(), 
			q.getProfessor().getName(), q.getCreatedAt(), images);
		
		return ResponseEntity.ok(dto);
	}

	@DeleteMapping("/{id}")
	@Transactional
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		log.info("Attempting to delete question with id: {}, user: {}", id, customPrincipal.getUserId());
		
		QuestionPost q = questionPostRepository.findById(id)
			.orElseThrow(() -> {
				log.error("Question not found with id: {}", id);
				return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
			});
		
		log.info("Found question: id={}, title={}, author={}", q.getId(), q.getTitle(), q.getUser().getId());
		
		if (!q.getUser().getId().equals(customPrincipal.getUserId())) {
			log.error("User {} is not authorized to delete question {}", customPrincipal.getUserId(), id);
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		
		List<QuestionImage> images = questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(id);
		log.info("Deleting {} images for question {}", images.size(), id);
		
		questionImageRepository.deleteAll(images);
		questionPostRepository.deleteById(id);
		
		log.info("Successfully deleted question {}", id);
		return ResponseEntity.noContent().build();
	}
}
