package com.qandding.domain.ai;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class GeminiVisionClient {
  private final WebClient webClient;
  private final String model;

  public GeminiVisionClient(@Value("${app.gemini.api-key}") String apiKey,
                            @Value("${app.gemini.model}") String model) {
    this.model = model;
    this.webClient = WebClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
        .defaultHeader("x-goog-api-key", apiKey)
        .build();
  }

  public Mono<String> generateWithImages(String instruction, List<ImagePart> images) {
    List<Map<String, Object>> parts = new ArrayList<>();
    parts.add(Map.of("text", instruction));
    for (ImagePart img : images) {
      parts.add(Map.of(
          "inline_data", Map.of(
              "mime_type", img.mimeType(),
              "data", Base64.getEncoder().encodeToString(img.bytes())
          )
      ));
    }

    Map<String, Object> body = Map.of(
        "contents", new Object[]{ Map.of("parts", parts) }
    );

    return webClient
        .post()
        .uri("/" + model + ":generateContent")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(Map.class)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
            .filter(ex -> ex instanceof WebClientResponseException w && w.getStatusCode().value() == 429)
            .maxBackoff(Duration.ofSeconds(15))
            .jitter(0.5)
        )
        .map(resp -> {
          try {
            var candidates = (List<?>) resp.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "";
            var candidate = (Map<?,?>) candidates.get(0);
            var content = (Map<?,?>) candidate.get("content");
            var partsResp = (List<?>) content.get("parts");
            var first = (Map<?,?>) partsResp.get(0);
            return String.valueOf(first.get("text"));
          } catch (Exception e) {
            return "";
          }
        });
  }

  public record ImagePart(String mimeType, byte[] bytes) {}
}
