package com.qandding.domain.ai.service;

import com.qandding.domain.ai.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiClient geminiClient;

    @Transactional(readOnly = true)
    public String generateText(String prompt) {
        return geminiClient.generateText(prompt).blockOptional().orElse("");
    }
}
