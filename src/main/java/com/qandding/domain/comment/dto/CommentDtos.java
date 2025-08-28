package com.qandding.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

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
		private final List<String> imageUrls;
		private final int replyCount;
		private final int totalCommentCount;

		// For QueryDSL constructor projection without images
		public Summary(Long id, String nickname, String content, LocalDateTime createdAt, int replyCount, int totalCommentCount) {
			this.id = id;
			this.nickname = nickname;
			this.content = content;
			this.createdAt = createdAt;
			this.imageUrls = null;
			this.replyCount = replyCount;
			this.totalCommentCount = totalCommentCount;
		}

		public static Summary from(Comment c) {
			return new Summary(c.getId(), c.getUser().getNickname(), c.getContent(), c.getCreatedAt(), 0, 0); // Default values, will be populated by query
		}
	}

    @Getter
    @AllArgsConstructor
    public static class Thread {
        private final Summary parent;
        private final java.util.List<Summary> replies;
    }
}
