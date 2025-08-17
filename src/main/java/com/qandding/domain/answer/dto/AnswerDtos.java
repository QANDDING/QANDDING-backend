package com.qandding.domain.answer.dto;

import java.time.LocalDateTime;

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

		public static Summary from(AnswerPost a) {
			return new Summary(a.getId(), a.getTitle(), a.getUser().getNickname(), a.getAiAnswer() != null, a.getCreatedAt());
		}
	}
}
