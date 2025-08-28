package com.qandding.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qandding.domain.ai.dto.GeneratedProblemDto;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.ResponseCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static java.rmi.server.LogStream.log;

@Service
@Slf4j
public class OpenAiProblemService {

    private final OpenAIClient client;

    public OpenAiProblemService(OpenAIClient client) {
        this.client = client;
    }

    public GeneratedProblemDto generateProblemsAndSolutions(String ocrText) {
        try {
            log.info("openai로그 호출.");
            String prompt = buildPrompt(ocrText);

            // Structured Outputs: DTO 클래스를 text(...)에 넘기면
            // 모델이 해당 스키마로만 답하고 SDK가 자동으로 역직렬화합니다.
            StructuredResponseCreateParams<GeneratedProblemDto> params =
                    ResponseCreateParams.builder()
                            .model(ChatModel.GPT_4_1_MINI) // 필요 시 GPT_4_1
                            .input(prompt)
                            .text(GeneratedProblemDto.class)
                            .build();

            StructuredResponse<GeneratedProblemDto> res = client.responses().create(params);

            // 첫 번째 메시지의 첫 번째 DTO 조각을 꺼냅니다.
            return res.output().stream()
                    .map(item -> item.message().orElse(null))
                    .filter(Objects::nonNull)
                    .flatMap(msg -> msg.content().stream()) // 여기서 이미 GeneratedProblemDto 타입
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No structured content in response")).asOutputText();

        } catch (Exception e) {
            log.error("Error generating problems and solutions from OpenAI for ocrText: {}", ocrText, e);
            throw new RuntimeException("Failed to generate problems and solutions from OpenAI. See logs for details.", e);
        }
    }

    private String buildPrompt(String ocrText) {
        return "You are an expert problem generator for study materials.\n" +
                "Based on the following problem text extracted via OCR, please perform the following tasks:\n" +
                "1. Analyze the problem's type, core concepts, and difficulty level.\n" +
                "2. Generate exactly 10 new problems that are similar in type, concept, and difficulty, but do not simply copy the original. Ensure they are fresh and unique problems.\n" +
                "3. For each of the 10 new problems, provide a concise, step-by-step solution (3-5 lines) and a final answer.\n\n" +
                "Original Problem Text:\n---start---\n" + ocrText + "\n---end---\n\n" +
                "Return ONLY valid JSON for this schema: " +
                "{\"problems\":[{\"newProblem\":\"string\",\"solution\":\"string\",\"answer\":\"string\"}]}\n" +
                "No extra text.";
    }
}
