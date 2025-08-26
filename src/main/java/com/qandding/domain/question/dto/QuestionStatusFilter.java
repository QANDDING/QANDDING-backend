package com.qandding.domain.question.dto;

public enum QuestionStatusFilter {
    ALL, // 전체
    ANSWERED, // 답변 완료 (AI 또는 멤버)
    UNANSWERED, // 미답변 (멤버)
    ADOPTED // 채택됨
}
