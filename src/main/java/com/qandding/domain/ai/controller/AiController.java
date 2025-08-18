package com.qandding.domain.ai.controller;

import com.qandding.domain.ai.service.AiService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "공통 AI 기능 API")
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    @Operation(summary = "텍스트 생성", description = "주어진 프롬프트로 AI가 텍스트를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Map<String, String>> generate(@RequestBody Map<String, String> req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof CustomUserPrincipal)) {
            return ResponseEntity.status(401).build();
        }

        String prompt = req.getOrDefault("prompt", "");
        String text = aiService.generateText(prompt);
        return ResponseEntity.ok(Map.of("text", text));
    }
}
