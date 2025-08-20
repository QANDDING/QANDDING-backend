package com.qandding.domain.question.service;

import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.dto.QuestionDtos;
import com.qandding.domain.question.entity.QuestionImage;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.event.QuestionCreatedEvent;
import com.qandding.domain.question.repository.QuestionImageRepository;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.subject.repository.SubjectRepository;
import com.qandding.domain.professor.repository.ProfessorRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.storage.S3PresignService;
import com.qandding.global.storage.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QuestionService {

    private final QuestionPostRepository questionPostRepository;
    private final QuestionImageRepository questionImageRepository;
    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final S3PresignService s3PresignService;
    private final AnswerPostRepository answerPostRepository;
    private final AnswerImageRepository answerImageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createQuestion(String title, String content, Long subjectId, Long professorId, List<String> imageUrls, Long userId) {
        log.debug("createQuestion() 호출됨 - title: {}, subjectId: {}, professorId: {}, userId: {}", title, subjectId, professorId, userId);
        // 안전 가드: DB 스키마가 TEXT로 남아 있는 환경 대비 길이 제한
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
            log.debug("제목 길이 200자로 제한됨");
        }
        // content는 LOB로 저장되므로 별도 길이 제한 없음
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var subject = subjectRepository.findById(subjectId).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        var professor = professorRepository.findById(professorId).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
        
        log.debug("DB 조회 완료 - user: {}, subject: {}, professor: {}", user.getNickname(), subject.getName(), professor.getName());

        QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));
        log.debug("질문 저장 완료 - questionId: {}", post.getId());

        if (imageUrls != null) {
            int i = 0;
            for (String url : imageUrls) {
                questionImageRepository.save(new QuestionImage(post, url, i++));
            }
            log.debug("이미지 URL {}개 저장 완료", imageUrls.size());
        }
        eventPublisher.publishEvent(new QuestionCreatedEvent(post.getId(), user.getId()));
        log.debug("QuestionCreatedEvent 발행 - questionId: {}, userId: {}", post.getId(), user.getId());
        log.debug("createQuestion() 반환 - questionId: {}", post.getId());
        return post.getId();
    }

    @Transactional
    public Long createQuestionWithImages(String title, String content, Long subjectId, Long professorId, List<MultipartFile> images, Long userId) throws IOException {
        log.debug("createQuestionWithImages() 호출됨 - title: {}, subjectId: {}, professorId: {}, userId: {}", title, subjectId, professorId, userId);
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
            log.debug("제목 길이 200자로 제한됨");
        }
        // content는 LOB로 저장되므로 별도 길이 제한 없음
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var subject = subjectRepository.findById(subjectId).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        var professor = professorRepository.findById(professorId).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
        log.debug("DB 조회 완료 - user: {}, subject: {}, professor: {}", user.getNickname(), subject.getName(), professor.getName());

        QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));
        log.debug("질문 저장 완료 - questionId: {}", post.getId());

        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    log.debug("이미지 업로드 시도 - fileName: {}", image.getOriginalFilename());
                    String imageUrl = s3UploadService.uploadImage(image);
                    imageUrls.add(imageUrl);
                    log.debug("이미지 업로드 성공 - imageUrl: {}", imageUrl);
                }
            }
            for (int i = 0; i < imageUrls.size(); i++) {
                questionImageRepository.save(new QuestionImage(post, imageUrls.get(i), i));
            }
            log.debug("이미지 URL {}개 저장 완료", imageUrls.size());
        }
        eventPublisher.publishEvent(new QuestionCreatedEvent(post.getId(), user.getId()));
        log.debug("QuestionCreatedEvent 발행 - questionId: {}, userId: {}", post.getId(), user.getId());
        log.debug("createQuestionWithImages() 반환 - questionId: {}", post.getId());
        return post.getId();
    }

    @Transactional
    public Long createQuestionWithFiles(String title, String content, Long subjectId, Long professorId, List<MultipartFile> files, Long userId) throws IOException {
        log.debug("createQuestionWithFiles() 호출됨 - title: {}, subjectId: {}, professorId: {}, userId: {}", title, subjectId, professorId, userId);
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
            log.debug("제목 길이 200자로 제한됨");
        }
        // content는 LOB로 저장되므로 별도 길이 제한 없음
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var subject = subjectRepository.findById(subjectId).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        var professor = professorRepository.findById(professorId).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
        log.debug("DB 조회 완료 - user: {}, subject: {}, professor: {}", user.getNickname(), subject.getName(), professor.getName());
        
        QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));
        log.debug("질문 저장 완료 - questionId: {}", post.getId());

        if (files != null && !files.isEmpty()) {
            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    log.debug("파일 업로드 시도 - fileName: {}", file.getOriginalFilename());
                    String url = s3UploadService.uploadFile(file); // 이미지와 PDF 모두 허용
                    uploadedUrls.add(url);
                    log.debug("파일 업로드 성공 - url: {}", url);
                }
            }
            for (int i = 0; i < uploadedUrls.size(); i++) {
                questionImageRepository.save(new QuestionImage(post, uploadedUrls.get(i), i));
            }
            log.debug("업로드된 파일 URL {}개 저장 완료", uploadedUrls.size());
        }

        eventPublisher.publishEvent(new QuestionCreatedEvent(post.getId(), user.getId()));
        log.debug("QuestionCreatedEvent 발행 - questionId: {}, userId: {}", post.getId(), user.getId());
        log.debug("createQuestionWithFiles() 반환 - questionId: {}", post.getId());
        return post.getId();
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        log.debug("deleteQuestion() 호출됨 - questionId: {}, userId: {}", questionId, userId);
        QuestionPost q = questionPostRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        log.debug("질문 조회 완료 - questionId: {}", q.getId());
        if (!q.getUser().getId().equals(userId)) {
            log.warn("질문 삭제 권한 없음 - questionId: {}, userId: {}, ownerId: {}", questionId, userId, q.getUser().getId());
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }
        List<QuestionImage> images = questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(questionId);
        log.debug("질문 이미지 {}개 조회 완료", images.size());
        questionImageRepository.deleteAll(images);
        log.debug("질문 이미지 삭제 완료");
        questionPostRepository.deleteById(questionId);
        log.debug("질문 삭제 완료 - questionId: {}", questionId);
        log.debug("deleteQuestion() 반환 - void");
    }

    public QuestionDtos.Detail getQuestionDetail(Long questionId) {
        log.debug("getQuestionDetail() 호출됨 - questionId: {}", questionId);
        QuestionPost q = questionPostRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        log.debug("질문 조회 완료 - questionId: {}", q.getId());
        
        List<String> images = questionImageRepository
                .findByQuestionPostIdOrderBySortOrderAsc(questionId)
                .stream()
                .map(qi -> {
                    log.debug("Presigning URL for image: {}", qi.getUrl());
                    String presignedUrl = s3PresignService.presignGetUrlByUrl(qi.getUrl());
                    log.debug("Presigned URL generated: {}", presignedUrl);
                    return presignedUrl;
                })
                .collect(Collectors.toList());
        log.debug("질문 이미지 {}개 Presigned URL 생성 완료", images.size());
        
        QuestionDtos.Detail detailDto = new QuestionDtos.Detail(q.getId(), q.getTitle(), q.getContent(),
                q.getUser().getNickname(), q.getSubject().getName(),
                q.getProfessor().getName(), q.getCreatedAt(), images);
        log.debug("getQuestionDetail() 반환 - questionId: {}", detailDto.getId());
        return detailDto;
    }

    // getQuestionDetailWithAnswers removed: use Answer Feed API instead

    public List<String> getQuestionImages(Long questionId) {
        log.debug("getQuestionImages() 호출됨 - questionId: {}", questionId);
        List<String> images = questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(questionId)
                .stream()
                .map(qi -> {
                    log.debug("Presigning URL for image: {}", qi.getUrl());
                    String presignedUrl = s3PresignService.presignGetUrlByUrl(qi.getUrl());
                    log.debug("Presigned URL generated: {}", presignedUrl);
                    return presignedUrl;
                })
                .collect(Collectors.toList());
        log.debug("getQuestionImages() 반환 - 이미지 {}개", images.size());
        return images;
    }
}
