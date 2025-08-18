package com.qandding.domain.ai.controller;

import com.qandding.global.common.paging.PageResponse;
import com.qandding.domain.ai.dto.AiAnswerDtos;
import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.ai.repository.AiAnswerRepository;
import com.qandding.domain.ai.HuggingFaceClient;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.storage.S3UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ai-answers")
@Tag(name = "AI 답변", description = "AI 답변 생성 및 관리 API")
public class AiAnswerController {
	private final AiAnswerRepository aiAnswerRepository;
	private final QuestionPostRepository questionPostRepository;
	private final AnswerPostRepository answerPostRepository;
	private final UserRepository userRepository;
	private final HuggingFaceClient huggingFaceClient;
	private final S3UploadService s3UploadService;

	public AiAnswerController(AiAnswerRepository aiAnswerRepository,
	                         QuestionPostRepository questionPostRepository,
	                         AnswerPostRepository answerPostRepository,
	                         UserRepository userRepository,
	                         HuggingFaceClient huggingFaceClient,
	                         S3UploadService s3UploadService) {
		this.aiAnswerRepository = aiAnswerRepository;
		this.questionPostRepository = questionPostRepository;
		this.answerPostRepository = answerPostRepository;
		this.userRepository = userRepository;
		this.huggingFaceClient = huggingFaceClient;
		this.s3UploadService = s3UploadService;
	}

	public record CreateAiAnswerRequest(
			@NotBlank Long questionPostId,
			@NotBlank String title,
			@NotBlank String content
	) {}

	public record GenerateAndSaveAiAnswerRequest(
			@NotBlank Long questionPostId,
			@NotBlank String prompt,
			String title  // 선택적, 없으면 자동 생성
	) {}

	public record GenerateAndSaveAiAnswerWithImageRequest(
			@NotBlank Long questionPostId,
			@NotBlank String prompt,
			String title  // 선택적, 없으면 자동 생성
	) {}

	@PostMapping
	@Transactional
	public ResponseEntity<Long> create(@RequestBody CreateAiAnswerRequest req) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		log.info("Creating AI answer for question: {}, user: {}", req.questionPostId(), customPrincipal.getUserId());
		
		// 사용자 정보 조회
		User user = userRepository.findById(customPrincipal.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		
		// 질문 존재 여부 확인
		var questionPost = questionPostRepository.findById(req.questionPostId())
			.orElseThrow(() -> {
				log.error("Question not found with id: {}", req.questionPostId());
				return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
			});
		
		// AI 답변 저장
		AiAnswer aiAnswer = aiAnswerRepository.save(
			new AiAnswer(req.title(), req.content())
		);
		
		log.info("Created AI answer with id: {}", aiAnswer.getId());
		
		// AnswerPost에 AI 답변 저장
		AnswerPost answerPost = answerPostRepository.save(
			new AnswerPost(user, questionPost, aiAnswer, req.title(), req.content())
		);
		
		log.info("Saved AI answer to AnswerPost with id: {}", answerPost.getId());
		
		return ResponseEntity.ok(answerPost.getId());
	}

	@PostMapping("/generate-and-save")
	@Transactional
	public ResponseEntity<AiAnswerDtos.Detail> generateAndSave(@RequestBody GenerateAndSaveAiAnswerRequest req) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		log.info("Generating and saving AI answer for question: {}, user: {}", req.questionPostId(), customPrincipal.getUserId());
		
		// 사용자 정보 조회
		User user = userRepository.findById(customPrincipal.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		
		// 질문 존재 여부 확인
		var questionPost = questionPostRepository.findById(req.questionPostId())
			.orElseThrow(() -> {
				log.error("Question not found with id: {}", req.questionPostId());
				return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
			});
		
		try {
			log.info("Starting AI response generation for prompt: {}", req.prompt());
			
			// AI 응답 생성
			String aiResponse = huggingFaceClient.generateText(req.prompt()).blockOptional().orElse("");
			log.info("AI response generated, length: {}", aiResponse != null ? aiResponse.length() : 0);
			
			if (aiResponse == null || aiResponse.isEmpty()) {
				log.error("Failed to generate AI response for question: {}", req.questionPostId());
				throw new BusinessException(ErrorCode.INTERNAL_ERROR);
			}
			
			// AI 응답 길이 제한 (MySQL TEXT 컬럼 제한: 65,535자)
			final int MAX_CONTENT_LENGTH = 65000;
			if (aiResponse.length() > MAX_CONTENT_LENGTH) {
				log.warn("AI response too long ({} chars), truncating to {} chars", aiResponse.length(), MAX_CONTENT_LENGTH);
				aiResponse = aiResponse.substring(0, MAX_CONTENT_LENGTH) + "\n\n[내용이 너무 길어 일부가 생략되었습니다.]";
			}
			
			// 제목이 없으면 자동 생성
			String title = req.title();
			if (title == null || title.isBlank()) {
				title = "AI 답변 - " + questionPost.getTitle();
			}
			// 제목 길이도 제한 (200자)
			if (title.length() > 200) {
				title = title.substring(0, 197) + "...";
			}
			log.info("Using title: {}", title);
			
			// AI 답변 저장
			log.info("Saving AI answer to database...");
			AiAnswer aiAnswer = aiAnswerRepository.save(
				new AiAnswer(title, aiResponse)
			);
			
			log.info("Generated AI answer with id: {}", aiAnswer.getId());
			
			// AnswerPost에 AI 답변 저장
			log.info("Saving AnswerPost to database...");
			AnswerPost answerPost = answerPostRepository.save(
				new AnswerPost(user, questionPost, aiAnswer, title, aiResponse)
			);
			
			log.info("Saved AI answer to AnswerPost with id: {}", answerPost.getId());
			
			// 상세 정보 반환
			log.info("Returning AI answer detail for id: {}", aiAnswer.getId());
			return ResponseEntity.ok(AiAnswerDtos.Detail.from(aiAnswer));
			
		} catch (Exception e) {
			log.error("Error generating and saving AI answer for question {}: {}", req.questionPostId(), e.getMessage(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	@PostMapping(value = "/generate-and-save-with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Transactional
	@Operation(summary = "이미지와 함께 AI 답변 생성", description = "이미지를 첨부하여 AI 답변을 생성하고 저장합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "AI 답변 생성 성공", 
			content = @Content(schema = @Schema(implementation = AiAnswerDtos.Detail.class))),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증 필요"),
		@ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "서버 오류")
	})
	public ResponseEntity<AiAnswerDtos.Detail> generateAndSaveWithImage(
			@Parameter(description = "질문 ID", required = true) @RequestParam("questionPostId") Long questionPostId,
			@Parameter(description = "AI에게 보낼 프롬프트", required = true) @RequestParam("prompt") String prompt,
			@Parameter(description = "답변 제목 (선택사항)") @RequestParam(value = "title", required = false) String title,
			@Parameter(description = "분석할 이미지 파일", required = false, 
				content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) 
			@RequestParam(value = "image", required = false) MultipartFile image) {
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}
		
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof CustomUserPrincipal customPrincipal)) {
			return ResponseEntity.status(401).build();
		}
		
		log.info("Generating and saving AI answer with image for question: {}, user: {}", questionPostId, customPrincipal.getUserId());
		
		// 사용자 정보 조회
		User user = userRepository.findById(customPrincipal.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		
		// 질문 존재 여부 확인
		var questionPost = questionPostRepository.findById(questionPostId)
			.orElseThrow(() -> {
				log.error("Question not found with id: {}", questionPostId);
				return new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
			});
		
		try {
			log.info("Starting AI response generation for prompt: {}", prompt);
			
			String aiResponse;
			
			// 이미지가 있으면 이미지와 함께 AI 응답 생성
			if (image != null && !image.isEmpty()) {
				log.info("Processing image for AI analysis: filename={}, size={}", image.getOriginalFilename(), image.getSize());
				
				// 이미지를 S3에 업로드
				String imageUrl = s3UploadService.uploadImage(image);
				log.info("Image uploaded to S3: {}", imageUrl);
				
				// 이미지 바이트 배열을 사용하여 AI 응답 생성
				aiResponse = huggingFaceClient.generateTextWithImageBytes(prompt, image.getBytes()).blockOptional().orElse("");
				log.info("AI response generated with image, length: {}", aiResponse != null ? aiResponse.length() : 0);
			} else {
				// 이미지가 없으면 텍스트만으로 AI 응답 생성
				aiResponse = huggingFaceClient.generateText(prompt).blockOptional().orElse("");
				log.info("AI response generated without image, length: {}", aiResponse != null ? aiResponse.length() : 0);
			}
			
			if (aiResponse == null || aiResponse.isEmpty()) {
				log.error("Failed to generate AI response for question: {}", questionPostId);
				throw new BusinessException(ErrorCode.INTERNAL_ERROR);
			}
			
			// AI 응답 길이 제한 (MySQL TEXT 컬럼 제한: 65,535자)
			final int MAX_CONTENT_LENGTH = 65000;
			if (aiResponse.length() > MAX_CONTENT_LENGTH) {
				log.warn("AI response too long ({} chars), truncating to {} chars", aiResponse.length(), MAX_CONTENT_LENGTH);
				aiResponse = aiResponse.substring(0, MAX_CONTENT_LENGTH) + "\n\n[내용이 너무 길어 일부가 생략되었습니다.]";
			}
			
			// 제목이 없으면 자동 생성
			String finalTitle = title;
			if (finalTitle == null || finalTitle.isBlank()) {
				finalTitle = "AI 답변 - " + questionPost.getTitle();
			}
			// 제목 길이도 제한 (200자)
			if (finalTitle.length() > 200) {
				finalTitle = finalTitle.substring(0, 197) + "...";
			}
			log.info("Using title: {}", finalTitle);
			
			// AI 답변 저장
			log.info("Saving AI answer to database...");
			AiAnswer aiAnswer = aiAnswerRepository.save(
				new AiAnswer(finalTitle, aiResponse)
			);
			
			log.info("Generated AI answer with id: {}", aiAnswer.getId());
			
			// AnswerPost에 AI 답변 저장
			log.info("Saving AnswerPost to database...");
			AnswerPost answerPost = answerPostRepository.save(
				new AnswerPost(user, questionPost, aiAnswer, finalTitle, aiResponse)
			);
			
			log.info("Saved AI answer to AnswerPost with id: {}", answerPost.getId());
			
			// 상세 정보 반환
			log.info("Returning AI answer detail for id: {}", aiAnswer.getId());
			return ResponseEntity.ok(AiAnswerDtos.Detail.from(aiAnswer));
			
		} catch (IOException e) {
			log.error("Error processing image for AI answer generation: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		} catch (Exception e) {
			log.error("Error generating and saving AI answer for question {}: {}", questionPostId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.INTERNAL_ERROR);
		}
	}

	@GetMapping
	public ResponseEntity<PageResponse<AiAnswerDtos.Summary>> list(
			@RequestParam Long questionPostId,
			@PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		
		log.info("Fetching AI answers for question: {}, page: {}, size: {}", 
			questionPostId, pageable.getPageNumber(), pageable.getPageSize());
		
		// 정렬 파라미터 검증 및 수정
		Pageable validatedPageable = validateAndFixPageable(pageable);
		
		// 효율적인 페이징 쿼리 사용
		Page<AiAnswer> aiAnswerPage = aiAnswerRepository.findByQuestionPostId(questionPostId, validatedPageable);
		
		List<AiAnswerDtos.Summary> content = aiAnswerPage.getContent().stream()
			.map(AiAnswerDtos.Summary::from)
			.toList();
		
		// Page<AiAnswer>를 Page<AiAnswerDtos.Summary>로 변환
		Page<AiAnswerDtos.Summary> summaryPage = new PageImpl<>(content, validatedPageable, aiAnswerPage.getTotalElements());
		
		log.info("Found {} AI answers for question {}, total: {}", 
			content.size(), questionPostId, aiAnswerPage.getTotalElements());
		
		return ResponseEntity.ok(PageResponse.of(summaryPage));
	}
	
	/**
	 * Pageable의 정렬을 검증하고 안전한 정렬로 수정
	 */
	private Pageable validateAndFixPageable(Pageable pageable) {
		// AiAnswer 엔티티의 허용된 정렬 필드들
		Set<String> allowedSortFields = Set.of("id", "title", "createdAt", "updatedAt");
		
		List<org.springframework.data.domain.Sort.Order> validatedOrders = pageable.getSort().stream()
			.filter(order -> allowedSortFields.contains(order.getProperty()))
			.collect(Collectors.toList());
		
		// 유효한 정렬이 없으면 기본값 사용
		if (validatedOrders.isEmpty()) {
			validatedOrders = List.of(new org.springframework.data.domain.Sort.Order(
				org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
			));
		}
		
		return org.springframework.data.domain.PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			org.springframework.data.domain.Sort.by(validatedOrders)
		);
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	public ResponseEntity<AiAnswerDtos.Detail> get(@PathVariable Long id) {
		log.info("Fetching AI answer with id: {}", id);
		
		AiAnswer answer = aiAnswerRepository.findById(id)
			.orElseThrow(() -> {
				log.error("AI answer not found with id: {}", id);
				return new BusinessException(ErrorCode.ANSWER_NOT_FOUND);
			});
		
		log.info("Found AI answer: id={}, title={}", answer.getId(), answer.getTitle());
		
		return ResponseEntity.ok(AiAnswerDtos.Detail.from(answer));
	}
}
