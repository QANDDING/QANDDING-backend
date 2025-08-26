package com.qandding.domain.comment.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReplyCreateRequest {
    private Long parentCommentId;
    private String content;
    private List<String> imageUrls;
}
