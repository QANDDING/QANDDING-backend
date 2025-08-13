package com.qandding.comment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentImage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_image_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false)
	private Comment comment;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(name = "sort_order")
	private Integer sortOrder;

	public CommentImage(Comment comment, String url, Integer sortOrder) {
		this.comment = comment;
		this.url = url;
		this.sortOrder = sortOrder;
	}
}
