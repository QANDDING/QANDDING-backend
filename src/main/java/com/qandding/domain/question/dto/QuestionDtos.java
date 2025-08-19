package com.qandding.domain.question.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.question.entity.QuestionPost;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public class QuestionDtos {
	@Getter
	@AllArgsConstructor
    @Schema(name = "QuestionSummary", description = "질문 목록 아이템")
    public static class Summary {
        @Schema(description = "질문 ID", example = "123")
        private final Long id;
        @Schema(description = "제목", example = "적분문제 질문드립니다")
        private final String title;
        @Schema(description = "작성자 닉네임", example = "홍길동")
        private final String authorNickname;
        @Schema(description = "과목명", example = "미적분학")
        private final String subjectName;
        @Schema(description = "교수명", example = "김교수")
        private final String professorName;
        @Schema(description = "작성일시", example = "2025-08-19T12:34:56")
        private final LocalDateTime createdAt;
        @Schema(description = "채택 여부", example = "true")
        @JsonProperty("accept")
        private final boolean accepted;

        public static Summary from(QuestionPost q) {
            // 기본값으로 accepted=false (Query 결과 기반이 아닌 단건 변환 시)
            return new Summary(q.getId(), q.getTitle(), q.getUser().getNickname(), q.getSubject().getName(), q.getProfessor().getName(), q.getCreatedAt(), false);
        }
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "QuestionDetail", description = "질문 단건 상세")
    public static class Detail {
        @Schema(description = "질문 ID")
        private final Long id;
        @Schema(description = "제목")
        private final String title;
        @Schema(description = "내용")
        private final String content;
        @Schema(description = "작성자 닉네임")
        private final String authorNickname;
        @Schema(description = "과목명")
        private final String subjectName;
        @Schema(description = "교수명")
        private final String professorName;
        @Schema(description = "작성일시")
        private final LocalDateTime createdAt;
        @Schema(description = "질문 이미지 URL 목록")
        private final List<String> imageUrls;
        @Schema(description = "채택 여부", example = "false")
        @com.fasterxml.jackson.annotation.JsonProperty("accept")
        private final boolean accepted;

		// imageUrls가 없는 생성자 추가
        public Detail(Long id, String title, String content, String authorNickname, 
                       String subjectName, String professorName, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.authorNickname = authorNickname;
            this.subjectName = subjectName;
            this.professorName = professorName;
            this.createdAt = createdAt;
            this.imageUrls = null;
            this.accepted = false;
        }
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "QuestionDetailWithAnswers", description = "질문 상세 + 답변 피드")
    public static class DetailWithAnswers {
        @Schema(description = "질문 ID")
        private final Long id;
        @Schema(description = "제목")
        private final String title;
        @Schema(description = "내용")
        private final String content;
        @Schema(description = "작성자 닉네임")
        private final String authorNickname;
        @Schema(description = "과목명")
        private final String subjectName;
        @Schema(description = "교수명")
        private final String professorName;
        @Schema(description = "작성일시")
        private final LocalDateTime createdAt;
        @Schema(description = "질문 이미지 URL 목록")
        private final List<String> imageUrls;
        @Schema(description = "채택 여부", example = "false")
        @com.fasterxml.jackson.annotation.JsonProperty("accept")
        private final boolean accepted;
        @Schema(description = "답변 피드(채택 여부 포함)")
        private final com.qandding.domain.answer.dto.AnswerDtos.Combined answers;
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "QuestionPageResponse", description = "질문 목록 페이징 응답")
    public static class PageResponseSummary {
        @io.swagger.v3.oas.annotations.media.ArraySchema(arraySchema = @Schema(description = "질문 목록"))
        private final java.util.List<QuestionDtos.Summary> content;
        @Schema(description = "현재 페이지(0-based)", example = "0")
        private final int page;
        @Schema(description = "페이지 크기", example = "10")
        private final int size;
        @Schema(description = "전체 아이템 수", example = "123")
        private final long totalElements;
        @Schema(description = "전체 페이지 수", example = "7")
        private final int totalPages;
    }
}
