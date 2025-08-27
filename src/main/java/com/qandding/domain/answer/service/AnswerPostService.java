package com.qandding.domain.answer.service;

import com.qandding.domain.answer.dto.AnswerPostDtos;
import com.qandding.domain.answer.entity.AnswerImage;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.AnswerType;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.storage.S3PresignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnswerPostService {

    private final AnswerPostRepository answerPostRepository;
    private final QuestionPostRepository questionPostRepository;
    private final UserRepository userRepository;
    private final AnswerImageRepository answerImageRepository;
    private final S3PresignService s3PresignService;

    @Transactional
    public Long createAnswerPostForUserWithImageUrls(Long questionPostId, String title, String content, List<String> imageUrls, Long userId) {
        log.debug("createAnswerPostForUserWithImageUrls() called - questionPostId: {}, userId: {}", questionPostId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost question = questionPostRepository.findById(questionPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // Create AnswerPost with USER type
        AnswerPost answerPost = AnswerPost.createForUser(question, user, title, content);
        answerPostRepository.save(answerPost);
        log.debug("AnswerPost (USER) saved - answerPostId: {}", answerPost.getId());

        if (imageUrls != null && !imageUrls.isEmpty()) {
            int i = 0;
            for (String url : imageUrls) {
                answerImageRepository.save(new AnswerImage(answerPost, url, i++));
            }
            log.debug("Image URLs {} saved.", imageUrls.size());
        }

        return answerPost.getId();
    }

    @Transactional
    public void deleteAnswerPost(Long answerPostId, Long userId) {
        log.debug("deleteAnswerPost() called - answerPostId: {}, userId: {}", answerPostId, userId);
        AnswerPost answerPost = answerPostRepository.findById(answerPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        log.debug("AnswerPost found - answerPostId: {}", answerPost.getId());

        // Check if it's a USER answer
        if (answerPost.getAnswerType() != AnswerType.USER) {
            log.warn("Attempted to delete a non-user answer - answerPostId: {}, type: {}", answerPostId, answerPost.getAnswerType());
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }
        
        // Check ownership
        if (answerPost.getAuthor() == null || !answerPost.getAuthor().getId().equals(userId)) {
            log.warn("Answer deletion forbidden - answerPostId: {}, userId: {}, authorId: {}", answerPostId, userId, answerPost.getAuthor() != null ? answerPost.getAuthor().getId() : "null");
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }

        // TODO: Add logic to delete images from S3 if required.
        answerImageRepository.deleteAllByAnswerPostId(answerPostId);
        answerPostRepository.deleteById(answerPostId);
        log.info("Successfully deleted user answer (AnswerPost) {}", answerPostId);
    }

    public AnswerPostDtos.Detail getAnswerPostDetail(Long answerPostId) {
        log.debug("getAnswerPostDetail() called - answerPostId: {}", answerPostId);
        AnswerPost answerPost = answerPostRepository.findById(answerPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        
        if (answerPost.getAnswerType() != AnswerType.USER) {
            log.warn("Attempted to get detail for a non-user answer - answerPostId: {}, type: {}", answerPostId, answerPost.getAnswerType());
            throw new BusinessException(ErrorCode.ANSWER_NOT_FOUND);
        }

        log.debug("AnswerPost found - answerPostId: {}", answerPost.getId());

        List<String> imageUrls = new ArrayList<>();
        var images = answerImageRepository.findByAnswerPostIdOrderBySortOrderAsc(answerPost.getId());
        log.debug("{} images found", images.size());
        for (var ai : images) {
            log.debug("Presigning URL for image: {}", ai.getUrl());
            imageUrls.add(s3PresignService.presignGetUrlByUrl(ai.getUrl()));
        }
        
        User author = answerPost.getAuthor();
        String authorNickname = (author != null) ? author.getNickname() : "Unknown";

        AnswerPostDtos.Detail detailDto = new AnswerPostDtos.Detail(
                answerPost.getId(), answerPost.getTitle(), answerPost.getContent(), authorNickname, answerPost.getCreatedAt(), imageUrls
        );
        log.debug("getAnswerPostDetail() returning - answerPostId: {}", detailDto.getId());
        return detailDto;
    }
}