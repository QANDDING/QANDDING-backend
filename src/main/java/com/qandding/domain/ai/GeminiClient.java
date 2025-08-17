package com.qandding.domain.ai;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class GeminiClient {
	private final WebClient webClient;
	private final String model;

	public GeminiClient(@Value("${app.gemini.api-key}") String apiKey,
	                   @Value("${app.gemini.model}") String model) {
		this.model = model;
		this.webClient = WebClient.builder()
			.baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
			.defaultHeader("x-goog-api-key", apiKey)
			.build();
	}

	public Mono<String> generateText(String prompt) {
		Map<String, Object> body = Map.of(
			"contents", new Object[]{ Map.of("parts", new Object[]{ Map.of("text", prompt) }) }
		);
		return webClient
			.post()
			.uri("/" + model + ":generateContent")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(body)
			.retrieve()
			.bodyToMono(Map.class)
			.map(resp -> {
				try {
					var candidates = (java.util.List<?>) resp.get("candidates");
					if (candidates == null || candidates.isEmpty()) return "";
					var candidate = (Map<?,?>) candidates.get(0);
					var content = (Map<?,?>) candidate.get("content");
					var parts = (java.util.List<?>) content.get("parts");
					var first = (Map<?,?>) parts.get(0);
					return String.valueOf(first.get("text"));
				} catch (Exception e) { return ""; }
			});
	}
}
