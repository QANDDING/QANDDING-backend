package com.qandding.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedProblemDto {
    private List<ProblemDetail> problems;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemDetail {
        private String newProblem; // 새로 생성된 문제
        private String solution;   // 풀이
        private String answer;     // 정답
    }
}
