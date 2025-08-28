package com.qandding.domain.user.service;

import com.qandding.domain.user.dto.UserPostDtos;
import com.qandding.domain.user.repository.UserPostQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

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
     * @param size 페이지 크기 (컨트롤러에서 10으로 고정)
     * @return 사용자가 작성한 글 목록
     */
    public UserPostDtos.UserPostsResponse getUserPosts(Long userId, int page, int size, String keyword, String postType) {
        log.info("사용자 통합 글 조회 요청 - userId: {}, page: {}, size: {}, keyword: {}, postType: {}", userId, page, size, keyword, postType);

        Pageable pageable = PageRequest.of(page, size);

        // 통합된 글 목록을 Repository에서 조회
        Page<UserPostQueryRepository.UnifiedPostProjection> postPage = userPostQueryRepository.findUnifiedPostsByUserId(userId, pageable, keyword, postType);

        // Projection을 DTO로 변환 (생성자 사용)
        var unifiedPostDtos = postPage.getContent().stream()
                .map(p -> new UserPostDtos.UnifiedPostDto(
                        p.getPostType(),
                        p.getPostId(),
                        p.getTitle(),
                        p.getCreatedAt(),
                        p.getOriginalQuestionId()))
                .collect(Collectors.toList());

        var response = new UserPostDtos.UserPostsResponse(
                unifiedPostDtos,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages()
        );

        log.info("사용자 통합 글 조회 완료 - userId: {}, 조회된 글: {}개", userId, unifiedPostDtos.size());

        return response;
    }
}