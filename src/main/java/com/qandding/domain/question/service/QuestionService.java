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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // 안전 가드: DB 스키마가 TEXT로 남아 있는 환경 대비 길이 제한
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
        }
        // content는 LOB로 저장되므로 별도 길이 제한 없음
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var subject = subjectRepository.findById(subjectId).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        var professor = professorRepository.findById(professorId).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
        QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));
        if (imageUrls != null) {
            int i = 0;
            for (String url : imageUrls) {
                questionImageRepository.save(new QuestionImage(post, url, i++));
            }
        }
        eventPublisher.publishEvent(new QuestionCreatedEvent(post.getId(), user.getId()));
        return post.getId();
    }

    @Transactional
    public Long createQuestionWithImages(String title, String content, Long subjectId, Long professorId, List<MultipartFile> images, Long userId) throws IOException {
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
        }
        // content는 LOB로 저장되므로 별도 길이 제한 없음
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var subject = subjectRepository.findById(subjectId).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        var professor = professorRepository.findById(professorId).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
        QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));

        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String imageUrl = s3UploadService.uploadImage(image);
                    imageUrls.add(imageUrl);
                }
            }
            for (int i = 0; i < imageUrls.size(); i++) {
                questionImageRepository.save(new QuestionImage(post, imageUrls.get(i), i));
            }
        }
        eventPublisher.publishEvent(new QuestionCreatedEvent(post.getId(), user.getId()));
        return post.getId();
    }

    @Transactional
    public Long createQuestionWithFiles(String title, String content, Long subjectId, Long professorId, List<MultipartFile> files, Long userId) throws IOException {
        if (title != null && title.length() > 200) {
            title = title.substring(0, 200);
        }
        // content는 LOB로 저장되므로 별도 길이 제한 없음
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var subject = subjectRepository.findById(subjectId).orElseThrow(() -> new BusinessException(ErrorCode.SUBJECT_NOT_FOUND));
        var professor = professorRepository.findById(professorId).orElseThrow(() -> new BusinessException(ErrorCode.PROFESSOR_NOT_FOUND));
        QuestionPost post = questionPostRepository.save(new QuestionPost(user, professor, subject, title, content));

        if (files != null && !files.isEmpty()) {
            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String url = s3UploadService.uploadFile(file); // 이미지와 PDF 모두 허용
                    uploadedUrls.add(url);
                }
            }
            for (int i = 0; i < uploadedUrls.size(); i++) {
                questionImageRepository.save(new QuestionImage(post, uploadedUrls.get(i), i));
            }
        }

        eventPublisher.publishEvent(new QuestionCreatedEvent(post.getId(), user.getId()));
        return post.getId();
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        QuestionPost q = questionPostRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        if (!q.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }
        List<QuestionImage> images = questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(questionId);
        questionImageRepository.deleteAll(images);
        questionPostRepository.deleteById(questionId);
    }

    public QuestionDtos.Detail getQuestionDetail(Long questionId) {
        QuestionPost q = questionPostRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        List<String> images = questionImageRepository
                .findByQuestionPostIdOrderBySortOrderAsc(questionId)
                .stream().map(qi -> s3PresignService.presignGetUrlByUrl(qi.getUrl())).collect(Collectors.toList());
        return new QuestionDtos.Detail(q.getId(), q.getTitle(), q.getContent(),
                q.getUser().getNickname(), q.getSubject().getName(),
                q.getProfessor().getName(), q.getCreatedAt(), images);
    }

    public QuestionDtos.DetailWithAnswers getQuestionDetailWithAnswers(Long questionId, int page, int size) {
        QuestionPost q = questionPostRepository.findById(questionId).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        List<String> qImages = questionImageRepository
                .findByQuestionPostIdOrderBySortOrderAsc(questionId)
                .stream().map(qi -> s3PresignService.presignGetUrlByUrl(qi.getUrl())).collect(Collectors.toList());

        // 가장 최신 AI 답변을 우선 선택
        AnswerPost aiPost = answerPostRepository.findTopByQuestionPost_IdAndAiAnswerIsNotNullOrderByCreatedAtDesc(questionId);
        AnswerDtos.Detail aiDetail = null;
        if (aiPost != null) {
            List<String> imgs = answerImageRepository.findByAnswerPostIdOrderBySortOrderAsc(aiPost.getId())
                    .stream().map(ai -> s3PresignService.presignGetUrlByUrl(ai.getUrl())).toList();
            aiDetail = new AnswerDtos.Detail(aiPost.getId(), aiPost.getTitle(), aiPost.getContent(),
                    aiPost.getUser().getNickname(), aiPost.getCreatedAt(), imgs, true);
        }

        PageRequest p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var userPage = answerPostRepository.findByQuestionPost_IdAndAiAnswerIsNull(questionId, p);
        List<Long> ids = userPage.getContent().stream().map(AnswerPost::getId).toList();
        Map<Long, List<String>> imgMap = new HashMap<>();
        if (!ids.isEmpty()) {
            var imgs = answerImageRepository.findByAnswerPostIdInOrderBySortOrderAsc(ids);
            for (var ai : imgs) {
                imgMap.computeIfAbsent(ai.getAnswerPost().getId(), k -> new ArrayList<>()).add(s3PresignService.presignGetUrlByUrl(ai.getUrl()));
            }
        }
        var userDetails = userPage.getContent().stream().map(pst -> new AnswerDtos.Detail(
                pst.getId(), pst.getTitle(), pst.getContent(), pst.getUser().getNickname(), pst.getCreatedAt(),
                imgMap.getOrDefault(pst.getId(), List.of()), false
        )).collect(Collectors.toList());
        var detailPage = new org.springframework.data.domain.PageImpl<>(userDetails, p, userPage.getTotalElements());
        var combined = new AnswerDtos.Combined(aiDetail, com.qandding.global.common.paging.PageResponse.of(detailPage));

        return new QuestionDtos.DetailWithAnswers(
                q.getId(), q.getTitle(), q.getContent(), q.getUser().getNickname(), q.getSubject().getName(), q.getProfessor().getName(),
                q.getCreatedAt(), qImages, combined
        );
    }

    public List<String> getQuestionImages(Long questionId) {
        return questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(questionId)
                .stream().map(qi -> s3PresignService.presignGetUrlByUrl(qi.getUrl())).collect(Collectors.toList());
    }
}
