package com.qandding.domain.question.event;

public record QuestionUpdatedEvent(Long questionPostId, Long userId, boolean shouldRegenerateAi) {}
