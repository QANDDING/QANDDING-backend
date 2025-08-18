package com.qandding.domain.answer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "answer_image_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "answer_post_id", nullable = false)
  private AnswerPost answerPost;

  @Column(nullable = false, length = 500)
  private String url;

  @Column(name = "sort_order")
  private Integer sortOrder;

  public AnswerImage(AnswerPost answerPost, String url, Integer sortOrder) {
    this.answerPost = answerPost;
    this.url = url;
    this.sortOrder = sortOrder;
  }
}

