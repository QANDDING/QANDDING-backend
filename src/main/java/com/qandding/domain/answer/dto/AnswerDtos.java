package com.qandding.domain.answer.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.answer.entity.AnswerPost;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class AnswerDtos {
	@Getter
	@AllArgsConstructor
	public static class Summary {
		private final Long id;
		private final String title;
		private final String authorNickname;
		private final boolean hasAi;
		private final LocalDateTime createdAt;
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
    public static class Detail {
        private final Long id;
        private final String title;
        private final String content;
        private final String authorNickname;
        private final LocalDateTime createdAt;
        private final List<String> imageUrls;
        private final boolean ai;
    }

    @Getter
    @AllArgsConstructor
    public static class Combined {
        private final Detail ai;
        private final com.qandding.global.common.paging.PageResponse<Detail> users;
    }
}
