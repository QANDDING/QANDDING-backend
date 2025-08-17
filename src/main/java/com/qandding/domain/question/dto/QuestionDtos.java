package com.qandding.domain.question.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.qandding.domain.question.entity.QuestionPost;

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
		}
	}
}
