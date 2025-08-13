package com.qandding.question.presentation.dto;

import com.qandding.question.domain.QuestionPost;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class QuestionDtos {
	@Getter
	@AllArgsConstructor
	public static class Summary {
		private final Long id;
		private final String title;
		private final String authorNickname;
		private final String subjectName;
		private final String professorName;
		private final LocalDateTime createdAt;

		public static Summary from(QuestionPost q) {
			return new Summary(q.getId(), q.getTitle(), q.getUser().getNickname(), q.getSubject().getName(), q.getProfessor().getName(), q.getCreatedAt());
		}
	}

	@Getter
	@AllArgsConstructor
	public static class Detail {
		private final Long id;
		private final String title;
		private final String content;
		private final String authorNickname;
		private final String subjectName;
		private final String professorName;
		private final LocalDateTime createdAt;
		private final List<String> imageUrls;
	}
}
