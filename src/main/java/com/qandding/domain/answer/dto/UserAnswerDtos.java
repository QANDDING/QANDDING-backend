package com.qandding.domain.answer.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.answer.entity.UserAnswer;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class UserAnswerDtos {
    @Getter
    @AllArgsConstructor
    @Schema(name = "UserAnswerSummary", description = "사용자 답변 목록 아이템")
    public static class Summary {
        @Schema(description = "사용자 답변 ID", example = "501")
        private final Long id;
        @Schema(description = "제목", example = "제 풀이")
        private final String title;
        @Schema(description = "작성자 닉네임", example = "kim")
        private final String authorNickname;
        @Schema(description = "작성일시", example = "2025-08-19T12:00:00")
        private final LocalDateTime createdAt;

		public static Summary from(UserAnswer answer) {
			return new Summary(
				answer.getId(),
				answer.getTitle(),
				answer.getUser().getNickname(),
				answer.getCreatedAt()
			);
		}
	}

    @Getter
    @AllArgsConstructor
    @Schema(name = "UserAnswerDetail", description = "사용자 답변 상세")
    public static class Detail {
        @Schema(description = "사용자 답변 ID", example = "501")
        private final Long id;
        @Schema(description = "제목", example = "제 풀이")
        private final String title;
        @Schema(description = "내용", example = "이렇게 풉니다...")
        private final String content;
        @Schema(description = "작성자 닉네임", example = "kim")
        private final String authorNickname;
        @Schema(description = "작성일시", example = "2025-08-19T12:00:00")
        private final LocalDateTime createdAt;
        @Schema(description = "이미지 URL 목록")
        private final List<String> imageUrls;

		public static Detail from(UserAnswer answer) {
			return new Detail(
				answer.getId(),
				answer.getTitle(),
				answer.getContent(),
				answer.getUser().getNickname(),
				answer.getCreatedAt(),
				null
			);
		}
	}
}
