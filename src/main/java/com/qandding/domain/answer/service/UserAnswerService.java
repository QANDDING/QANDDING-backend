package com.qandding.domain.answer.service;

import com.qandding.domain.answer.dto.UserAnswerDtos;
import com.qandding.domain.answer.entity.AnswerImage;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.UserAnswer;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.answer.repository.UserAnswerRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.storage.S3PresignService;
import com.qandding.global.storage.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserAnswerService {

    private final UserAnswerRepository userAnswerRepository;
    private final AnswerPostRepository answerPostRepository;
    private final QuestionPostRepository questionPostRepository;
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final AnswerImageRepository answerImageRepository;
    private final S3PresignService s3PresignService;

    @Transactional
    public Long createUserAnswer(Long questionPostId, String title, String content, Long userId) {
        log.debug("createUserAnswer() 호출됨 - questionPostId: {}, title: {}, userId: {}", questionPostId, title, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        log.debug("DB 조회 완료 - user: {}, question: {}", user.getNickname(), question.getTitle());

        UserAnswer userAnswer = userAnswerRepository.save(new UserAnswer(user, question, title, content));
        log.debug("UserAnswer 저장 완료 - userAnswerId: {}", userAnswer.getId());
        answerPostRepository.save(new AnswerPost(user, question, title, content));
        log.debug("AnswerPost 저장 완료");

        log.info("Created user answer with id: {}", userAnswer.getId());
        log.debug("createUserAnswer() 반환 - userAnswerId: {}", userAnswer.getId());
        return userAnswer.getId();
    }

    @Transactional
    public Long createUserAnswerWithImages(Long questionPostId, String title, String content, List<MultipartFile> images, Long userId) {
        log.debug("createUserAnswerWithImages() 호출됨 - questionPostId: {}, title: {}, userId: {}", questionPostId, title, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        log.debug("DB 조회 완료 - user: {}, question: {}", user.getNickname(), question.getTitle());

        UserAnswer userAnswer = userAnswerRepository.save(new UserAnswer(user, question, title, content));
        log.debug("UserAnswer 저장 완료 - userAnswerId: {}", userAnswer.getId());
        AnswerPost answerPost = answerPostRepository.save(new AnswerPost(user, question, title, content));
        log.debug("AnswerPost 저장 완료 - answerPostId: {}", answerPost.getId());

        if (images != null && !images.isEmpty()) {
            int i = 0;
            for (MultipartFile image : images) {
                try {
                    if (!image.isEmpty()) {
                        log.debug("이미지 업로드 시도 - fileName: {}", image.getOriginalFilename());
                        String imageUrl = s3UploadService.uploadImage(image);
                        log.debug("이미지 업로드 성공 - imageUrl: {}", imageUrl);
                        answerImageRepository.save(new AnswerImage(answerPost, imageUrl, i++));
                        log.debug("AnswerImage 저장 완료");
                    }
                } catch (IOException e) {
                    log.error("Failed to upload image: {}", image.getOriginalFilename(), e);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }
            }
            log.debug("이미지 {}개 처리 완료", images.size());
        }
        log.debug("createUserAnswerWithImages() 반환 - userAnswerId: {}", userAnswer.getId());
        return userAnswer.getId();
    }

    @Transactional
    public Long createUserAnswerWithFiles(Long questionPostId, String title, String content, List<MultipartFile> files, Long userId) {
        log.debug("createUserAnswerWithFiles() 호출됨 - questionPostId: {}, title: {}, userId: {}", questionPostId, title, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        log.debug("DB 조회 완료 - user: {}, question: {}", user.getNickname(), question.getTitle());

        UserAnswer userAnswer = userAnswerRepository.save(new UserAnswer(user, question, title, content));
        log.debug("UserAnswer 저장 완료 - userAnswerId: {}", userAnswer.getId());
        AnswerPost answerPost = answerPostRepository.save(new AnswerPost(user, question, title, content));
        log.debug("AnswerPost 저장 완료 - answerPostId: {}", answerPost.getId());

        if (files != null && !files.isEmpty()) {
            int i = 0;
            for (MultipartFile file : files) {
                try {
                    if (!file.isEmpty()) {
                        log.debug("파일 업로드 시도 - fileName: {}", file.getOriginalFilename());
                        String url = s3UploadService.uploadFile(file); // 이미지/PDF 모두 허용
                        log.debug("파일 업로드 성공 - url: {}", url);
                        answerImageRepository.save(new AnswerImage(answerPost, url, i++));
                        log.debug("AnswerImage 저장 완료");
                    }
                } catch (IOException e) {
                    log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }
            }
            log.debug("파일 {}개 처리 완료", files.size());
        }
        log.debug("createUserAnswerWithFiles() 반환 - userAnswerId: {}", userAnswer.getId());
        return userAnswer.getId();
    }

    @Transactional
    public void deleteUserAnswer(Long userAnswerId, Long userId) {
        log.debug("deleteUserAnswer() 호출됨 - userAnswerId: {}, userId: {}", userAnswerId, userId);
        UserAnswer answer = userAnswerRepository.findById(userAnswerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        log.debug("UserAnswer 조회 완료 - userAnswerId: {}", answer.getId());

        if (!answer.getUser().getId().equals(userId)) {
            log.warn("답변 삭제 권한 없음 - userAnswerId: {}, userId: {}, ownerId: {}", userAnswerId, userId, answer.getUser().getId());
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }

        userAnswerRepository.deleteById(userAnswerId);
        log.info("Successfully deleted user answer {}", userAnswerId);
        log.debug("deleteUserAnswer() 반환 - void");
    }

    public UserAnswerDtos.Detail getUserAnswerDetail(Long userAnswerId) {
        log.debug("getUserAnswerDetail() 호출됨 - userAnswerId: {}", userAnswerId);
        UserAnswer answer = userAnswerRepository.findById(userAnswerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        log.debug("UserAnswer 조회 완료 - userAnswerId: {}", answer.getId());

        var related = answerPostRepository.findTopByUser_IdAndQuestionPost_IdAndTitleAndContentOrderByIdDesc(
                answer.getUser().getId(), answer.getQuestionPost().getId(), answer.getTitle(), answer.getContent()
        );
        log.debug("연관 AnswerPost 조회 - relatedPostId: {}", related != null ? related.getId() : "null");

        List<String> imageUrls = new ArrayList<>();
        if (related != null) {
            var images = answerImageRepository.findByAnswerPostIdOrderBySortOrderAsc(related.getId());
            log.debug("AnswerImage {}개 조회", images.size());
            for (var ai : images) {
                log.debug("Presigning URL for image: {}", ai.getUrl());
                imageUrls.add(s3PresignService.presignGetUrlByUrl(ai.getUrl()));
            }
        }
        UserAnswerDtos.Detail detailDto = new UserAnswerDtos.Detail(
                answer.getId(), answer.getTitle(), answer.getContent(), answer.getUser().getNickname(), answer.getCreatedAt(), imageUrls
        );
        log.debug("getUserAnswerDetail() 반환 - userAnswerId: {}", detailDto.getId());
        return detailDto;
    }
}
