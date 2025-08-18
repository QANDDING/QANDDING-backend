package com.qandding.domain.answer.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.answer.entity.UserAnswer;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class UserAnswerDtos {
	@Getter
	@AllArgsConstructor
	public static class Summary {
		private final Long id;
		private final String title;
		private final String authorNickname;
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
	public static class Detail {
		private final Long id;
		private final String title;
		private final String content;
		private final String authorNickname;
		private final LocalDateTime createdAt;
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
