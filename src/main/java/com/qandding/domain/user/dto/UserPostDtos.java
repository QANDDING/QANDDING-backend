package com.qandding.domain.user.dto;

import java.time.LocalDateTime;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.question.entity.QuestionPost;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserPostDtos {

    @Getter
    @NoArgsConstructor
    @Schema(description = "사용자가 작성한 질문글 정보")
    public static class QuestionPostDto {
        @Schema(description = "질문글 ID", example = "1")
        private Long questionPostId;
        
        @Schema(description = "질문글 제목", example = "수학 문제 질문입니다")
        private String title;
        
        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;

        public QuestionPostDto(QuestionPost questionPost) {
            this.questionPostId = questionPost.getId();
            this.title = questionPost.getTitle();
            this.createdAt = questionPost.getCreatedAt();
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "사용자가 작성한 답변글 정보")
    public static class AnswerPostDto {
        @Schema(description = "답변글 ID", example = "1")
        private Long answerPostId;
        
        @Schema(description = "답변글 제목", example = "수학 문제 답변입니다")
        private String title;
        
        @Schema(description = "생성 시간")
        private LocalDateTime createdAt;
        
        @Schema(description = "답변글이 달린 질문글 ID", example = "1")
        private Long questionPostId;

        public AnswerPostDto(AnswerPost answerPost) {
            this.answerPostId = answerPost.getId();
            this.title = answerPost.getTitle();
            this.createdAt = answerPost.getCreatedAt();
            this.questionPostId = answerPost.getQuestionPost().getId();
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "사용자가 작성한 글 통합 응답")
    public static class UserPostsResponse {
        @Schema(description = "질문글 목록")
        private java.util.List<QuestionPostDto> questions;
        
        @Schema(description = "답변글 목록")
        private java.util.List<AnswerPostDto> answers;
        
        @Schema(description = "현재 페이지", example = "0")
        private int page;
        
        @Schema(description = "페이지 크기", example = "10")
        private int size;
        
        @Schema(description = "전체 질문글 수", example = "25")
        private long totalQuestions;
        
        @Schema(description = "전체 답변글 수", example = "30")
        private long totalAnswers;
        
        @Schema(description = "전체 페이지 수", example = "3")
        private int totalPages;

        public UserPostsResponse(
                java.util.List<QuestionPostDto> questions,
                java.util.List<AnswerPostDto> answers,
                int page,
                int size,
                long totalQuestions,
                long totalAnswers,
                int totalPages) {
            this.questions = questions;
            this.answers = answers;
            this.page = page;
            this.size = size;
            this.totalQuestions = totalQuestions;
            this.totalAnswers = totalAnswers;
            this.totalPages = totalPages;
        }
    }
}
