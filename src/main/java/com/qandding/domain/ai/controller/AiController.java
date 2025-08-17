package com.qandding.domain.ai.controller;

import com.qandding.domain.ai.GeminiClient;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
	private final GeminiClient geminiClient;

	public AiController(GeminiClient geminiClient) {
		this.geminiClient = geminiClient;
	}

	@PostMapping("/generate")
	public ResponseEntity<Map<String, String>> generate(@AuthenticationPrincipal CustomUserPrincipal principal,
	                                                   @RequestBody Map<String, String> req) {
		String prompt = req.getOrDefault("prompt", "");
		String text = geminiClient.generateText(prompt).blockOptional().orElse("");
		return ResponseEntity.ok(Map.of("text", text));
	}
}
