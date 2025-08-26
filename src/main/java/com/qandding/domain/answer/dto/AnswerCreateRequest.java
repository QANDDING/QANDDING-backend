package com.qandding.domain.answer.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerCreateRequest {
    private Long questionPostId;
    private String title;
    private String content;
    private List<String> imageUrls;
}
