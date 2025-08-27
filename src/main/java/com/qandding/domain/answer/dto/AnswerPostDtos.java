package com.qandding.domain.answer.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.answer.entity.AnswerPost;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class AnswerPostDtos {
	@Getter
	@AllArgsConstructor
	public static class Summary {
		private final Long id;
		private final String title;
		private final String authorNickname;
		private final LocalDateTime createdAt;

		public static Summary from(AnswerPost answer) {
			return new Summary(
				answer.getId(),
				answer.getTitle(),
				answer.getAuthor().getNickname(),
				answer.getCreatedAt()
			);
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

		public static Detail from(AnswerPost answer) {
			return new Detail(
				answer.getId(),
				answer.getTitle(),
				answer.getContent(),
				answer.getAuthor().getNickname(),
				answer.getCreatedAt(),
				null
			);
		}
	}
}
