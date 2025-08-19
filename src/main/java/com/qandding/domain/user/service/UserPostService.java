package com.qandding.domain.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.user.dto.UserPostDtos;
import com.qandding.domain.user.repository.UserPostQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPostService {

    private final UserPostQueryRepository userPostQueryRepository;

    /**
     * 사용자가 작성한 질문글과 답변글을 페이징하여 조회
     * 
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 사용자가 작성한 글 목록
     */
    public UserPostDtos.UserPostsResponse getUserPosts(Long userId, int page, int size) {
        log.info("사용자 글 조회 요청 - userId: {}, page: {}, size: {}", userId, page, size);
        
        // 페이지 크기를 10개로 고정
        int fixedSize = 10;
        Pageable pageable = PageRequest.of(page, fixedSize);
        
        // 질문글과 답변글을 병렬로 조회
        Page<QuestionPost> questionPage = userPostQueryRepository.findQuestionPostsByUserId(userId, pageable);
        Page<AnswerPost> answerPage = userPostQueryRepository.findAnswerPostsByUserId(userId, pageable);
        
        // DTO 변환
        var questionDtos = questionPage.getContent().stream()
                .map(UserPostDtos.QuestionPostDto::new)
                .toList();
        
        var answerDtos = answerPage.getContent().stream()
                .map(UserPostDtos.AnswerPostDto::new)
                .toList();
        
        // 전체 페이지 수 계산 (질문글과 답변글 중 더 큰 값 사용)
        int totalPages = Math.max(questionPage.getTotalPages(), answerPage.getTotalPages());
        
        var response = new UserPostDtos.UserPostsResponse(
                questionDtos,
                answerDtos,
                page,
                fixedSize,
                questionPage.getTotalElements(),
                answerPage.getTotalElements(),
                totalPages
        );
        
        log.info("사용자 글 조회 완료 - userId: {}, 질문글: {}개, 답변글: {}개", 
                userId, questionDtos.size(), answerDtos.size());
        
        return response;
    }
}
