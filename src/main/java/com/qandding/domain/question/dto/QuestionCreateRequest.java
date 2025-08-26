package com.qandding.domain.question.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionCreateRequest {
    private String title;
    private String content;
    private Long subjectId;
    private Long professorId;
    private List<String> imageUrls;
}
