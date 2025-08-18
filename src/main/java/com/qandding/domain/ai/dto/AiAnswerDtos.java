package com.qandding.domain.ai.dto;

import java.time.LocalDateTime;

import com.qandding.domain.ai.entity.AiAnswer;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class AiAnswerDtos {
	@Getter
	@AllArgsConstructor
	public static class Summary {
		private final Long id;
		private final String title;
		private final LocalDateTime createdAt;

		public static Summary from(AiAnswer answer) {
			return new Summary(
				answer.getId(),
				answer.getTitle(),
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
		private final LocalDateTime createdAt;

		public static Detail from(AiAnswer answer) {
			return new Detail(
				answer.getId(),
				answer.getTitle(),
				answer.getContent(),
				answer.getCreatedAt()
			);
		}
	}
}
