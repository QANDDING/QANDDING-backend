package com.qandding.domain.answer.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.answer.entity.AnswerPost;

import lombok.AllArgsConstructor;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;

public class AnswerDtos {
    @Getter
    @AllArgsConstructor
    @Schema(name = "AnswerSummary", description = "답변 목록 아이템")
    public static class Summary {
		@Schema(description = "답변 ID", example = "22")
		private final Long id;
		@Schema(description = "제목", example = "제 풀이")
		private final String title;
		@Schema(description = "작성자 닉네임", example = "hong")
		private final String authorNickname;
		@Schema(description = "AI 답변 여부", example = "false")
		private final boolean hasAi;
		@Schema(description = "작성일시", example = "2025-08-19T12:40:00")
		private final LocalDateTime createdAt;
		@Schema(description = "답변 이미지 URL 목록")
		private final List<String> imageUrls;

		// For QueryDSL constructor projection without images
		public Summary(Long id, String title, String authorNickname, boolean hasAi, LocalDateTime createdAt) {
			this.id = id;
			this.title = title;
			this.authorNickname = authorNickname;
			this.hasAi = hasAi;
			this.createdAt = createdAt;
			this.imageUrls = null;
		}

		public static Summary from(AnswerPost a) {
			return new Summary(a.getId(), a.getTitle(), a.getUser().getNickname(), a.getAiAnswer() != null, a.getCreatedAt(), null);
		}
	}

    @Getter
    @AllArgsConstructor
    @Schema(name = "AnswerDetail", description = "사용자 답변 상세/피드 아이템")
    public static class Detail {
        @Schema(description = "답변 ID", example = "22")
        private final Long id;
        @Schema(description = "제목", example = "제 풀이")
        private final String title;
        @Schema(description = "내용", example = "이렇게 풉니다...")
        private final String content;
        @Schema(description = "작성자 닉네임", example = "hong")
        private final String authorNickname;
        @Schema(description = "작성일시", example = "2025-08-19T12:40:00")
        private final LocalDateTime createdAt;
        @Schema(description = "이미지 URL 목록")
        private final List<String> imageUrls;
        @Schema(description = "채택 여부", example = "true")
        @com.fasterxml.jackson.annotation.JsonProperty("accept")
        private final boolean accepted;
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "AiAnswerDetail", description = "AI 답변 상세")
    public static class Ai {
        @Schema(description = "AI 답변 ID", example = "11")
        private final Long id;
        @Schema(description = "제목", example = "AI 답변 - 적분 문제")
        private final String title;
        @Schema(description = "내용", example = "...")
        private final String content;
        @Schema(description = "작성자 닉네임", example = "AI")
        private final String authorNickname;
        @Schema(description = "작성일시", example = "2025-08-19T12:34:56")
        private final LocalDateTime createdAt;
    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "AnswerCombined", description = "AI 최상단 + 사용자 답변 페이징 응답")
    public static class Combined {
        @Schema(description = "AI 답변(없으면 null)")
        private final Ai ai;
        @Schema(description = "사용자 답변 페이징")
        private final com.qandding.global.common.paging.PageResponse<Detail> users;
    }
}
