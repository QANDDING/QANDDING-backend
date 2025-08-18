package com.qandding.domain.comment.entity;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.user.entity.User;
import com.qandding.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "comment_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "answer_post_id", nullable = false)
	private AnswerPost answerPost;


	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
    private java.util.List<Comment> replies = new java.util.ArrayList<>();

    public Comment(AnswerPost answerPost, User user, String content) {
        this.answerPost = answerPost;
        this.user = user;
        this.content = content;
    }

    public void setParent(Comment parent) {
        this.parent = parent;
    }
}
