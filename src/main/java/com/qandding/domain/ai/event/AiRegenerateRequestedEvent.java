package com.qandding.domain.ai.event;

import java.util.List;

public record AiRegenerateRequestedEvent(
    Long questionPostId,
    Long userId,
    String prompt,
    String title,
    List<String> fileUrls
) {}

