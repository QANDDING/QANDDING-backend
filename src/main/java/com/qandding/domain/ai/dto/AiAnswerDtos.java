package com.qandding.domain.ai.dto;

import com.qandding.domain.answer.entity.AnswerPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class AiAnswerDtos {

    @Getter
    @AllArgsConstructor
    @Schema(description = "AI 답변 요약 정보 DTO")
    public static class Summary {
        private Long id;
        private String title;
        private LocalDateTime createdAt;

        public static Summary from(AnswerPost answerPost) {
            return new Summary(
                answerPost.getId(),
                answerPost.getTitle(),
                answerPost.getCreatedAt()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "AI 답변 상세 정보 DTO")
    public static class Detail {
        private Long id;
        private String title;
        private String content;
        private LocalDateTime createdAt;
        private List<String> imageUrls;

        public static Detail from(AnswerPost answerPost) {
            return new Detail(
                answerPost.getId(),
                answerPost.getTitle(),
                answerPost.getContent(),
                answerPost.getCreatedAt(),
                List.of() // AI Answers do not have images in this context
            );
        }
    }
}