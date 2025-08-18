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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        UserAnswer userAnswer = userAnswerRepository.save(new UserAnswer(user, question, title, content));
        answerPostRepository.save(new AnswerPost(user, question, title, content));

        log.info("Created user answer with id: {}", userAnswer.getId());
        return userAnswer.getId();
    }

    @Transactional
    public Long createUserAnswerWithImages(Long questionPostId, String title, String content, List<MultipartFile> images, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        UserAnswer userAnswer = userAnswerRepository.save(new UserAnswer(user, question, title, content));
        AnswerPost answerPost = answerPostRepository.save(new AnswerPost(user, question, title, content));

        if (images != null && !images.isEmpty()) {
            int i = 0;
            for (MultipartFile image : images) {
                try {
                    if (!image.isEmpty()) {
                        String imageUrl = s3UploadService.uploadImage(image);
                        answerImageRepository.save(new AnswerImage(answerPost, imageUrl, i++));
                    }
                } catch (IOException e) {
                    log.error("Failed to upload image: {}", image.getOriginalFilename(), e);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }
            }
        }
        return userAnswer.getId();
    }

    @Transactional
    public Long createUserAnswerWithFiles(Long questionPostId, String title, String content, List<MultipartFile> files, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        UserAnswer userAnswer = userAnswerRepository.save(new UserAnswer(user, question, title, content));
        AnswerPost answerPost = answerPostRepository.save(new AnswerPost(user, question, title, content));

        if (files != null && !files.isEmpty()) {
            int i = 0;
            for (MultipartFile file : files) {
                try {
                    if (!file.isEmpty()) {
                        String url = s3UploadService.uploadFile(file); // 이미지/PDF 모두 허용
                        answerImageRepository.save(new AnswerImage(answerPost, url, i++));
                    }
                } catch (IOException e) {
                    log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }
            }
        }
        return userAnswer.getId();
    }

    @Transactional
    public void deleteUserAnswer(Long userAnswerId, Long userId) {
        UserAnswer answer = userAnswerRepository.findById(userAnswerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        if (!answer.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }

        userAnswerRepository.deleteById(userAnswerId);
        log.info("Successfully deleted user answer {}", userAnswerId);
    }

    public UserAnswerDtos.Detail getUserAnswerDetail(Long userAnswerId) {
        UserAnswer answer = userAnswerRepository.findById(userAnswerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        var related = answerPostRepository.findTopByUser_IdAndQuestionPost_IdAndTitleAndContentOrderByIdDesc(
                answer.getUser().getId(), answer.getQuestionPost().getId(), answer.getTitle(), answer.getContent()
        );
        List<String> imageUrls = new ArrayList<>();
        if (related != null) {
            var images = answerImageRepository.findByAnswerPostIdOrderBySortOrderAsc(related.getId());
            for (var ai : images) imageUrls.add(s3PresignService.presignGetUrlByUrl(ai.getUrl()));
        }
        return new UserAnswerDtos.Detail(
                answer.getId(), answer.getTitle(), answer.getContent(), answer.getUser().getNickname(), answer.getCreatedAt(), imageUrls
        );
    }
}
