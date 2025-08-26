package com.qandding.domain.comment.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateRequest {
    private Long answerPostId;
    private String content;
    private List<String> imageUrls;
}
