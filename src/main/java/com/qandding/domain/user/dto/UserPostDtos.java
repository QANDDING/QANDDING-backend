package com.qandding.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.question.entity.QuestionPost;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserPostDtos {

    @Getter
    @NoArgsConstructor
    @Schema(description = "사용자가 작성한 글(질문 또는 답변)의 통합 정보")
    public static class UnifiedPostDto {
        @Schema(description = "글의 종류 (QUESTION | ANSWER)", example = "QUESTION")
        private String postType;

        @Schema(description = "글 ID", example = "1")
        private Long postId;
        
        @Schema(description = "글 제목", example = "JPA 질문입니다")
        private String title;
        
        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;
        
        @Schema(description = "답변글인 경우, 원본 질문글의 ID", example = "123", nullable = true)
        private Long originalQuestionId;

        // Native Query Projection을 위한 생성자
        public UnifiedPostDto(String postType, Long postId, String title, LocalDateTime createdAt, Long originalQuestionId) {
            this.postType = postType;
            this.postId = postId;
            this.title = title;
            this.createdAt = createdAt;
            this.originalQuestionId = originalQuestionId;
        }

        // QuestionPost를 위한 생성자
        public UnifiedPostDto(QuestionPost questionPost) {
            this.postType = "QUESTION";
            this.postId = questionPost.getId();
            this.title = questionPost.getTitle();
            this.createdAt = questionPost.getCreatedAt();
            this.originalQuestionId = null; // 질문글은 원본 질문 ID가 없음
        }

        // AnswerPost를 위한 생성자
        public UnifiedPostDto(AnswerPost answerPost) {
            this.postType = "ANSWER";
            this.postId = answerPost.getId();
            this.title = answerPost.getTitle();
            this.createdAt = answerPost.getCreatedAt();
            this.originalQuestionId = answerPost.getQuestionPost().getId();
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "사용자가 작성한 글 통합 목록 응답")
    public static class UserPostsResponse {
        @Schema(description = "글 목록 (질문, 답변 통합 및 최신순 정렬)")
        private List<UnifiedPostDto> posts;
        
        @Schema(description = "현재 페이지", example = "0")
        private int page;
        
        @Schema(description = "페이지 크기", example = "10")
        private int size;
        
        @Schema(description = "전체 글 수", example = "55")
        private long totalElements;
        
        @Schema(description = "전체 페이지 수", example = "6")
        private int totalPages;

        public UserPostsResponse(List<UnifiedPostDto> posts, int page, int size, long totalElements, int totalPages) {
            this.posts = posts;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
    }
}
