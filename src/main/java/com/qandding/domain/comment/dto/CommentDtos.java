package com.qandding.domain.comment.dto;

import java.time.LocalDateTime;

import com.qandding.domain.comment.entity.Comment;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class CommentDtos {
	@Getter
	@AllArgsConstructor
	public static class Summary {
		private final Long id;
		private final String nickname;
		private final String content;
		private final LocalDateTime createdAt;

		public static Summary from(Comment c) {
			return new Summary(c.getId(), c.getUser().getNickname(), c.getContent(), c.getCreatedAt());
		}
	}
}
