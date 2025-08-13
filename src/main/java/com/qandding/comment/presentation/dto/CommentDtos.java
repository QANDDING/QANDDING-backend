package com.qandding.comment.presentation.dto;

import com.qandding.comment.domain.Comment;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class CommentDtos {
	@Getter
	@AllArgsConstructor
	public static class Summary {
		private final Long id;
		private final String authorNickname;
		private final String content;
		private final LocalDateTime createdAt;

		public static Summary from(Comment c) {
			return new Summary(c.getId(), c.getUser().getNickname(), c.getContent(), c.getCreatedAt());
		}
	}
}
