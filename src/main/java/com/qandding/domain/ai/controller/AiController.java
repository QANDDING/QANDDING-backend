package com.qandding.domain.ai.controller;

import com.qandding.domain.ai.service.AiService;
import com.qandding.domain.user.entity.CustomUserPrincipal;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
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
            @ApiResponse(responseCode = "401", description = "인증 실패"),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })

    public ResponseEntity<Map<String, String>> generate(@RequestBody Map<String, String> req,
            @AuthenticationPrincipal CustomUserPrincipal customPrincipal) {
        
        // JWT 토큰 검증 (Spring Security가 자동으로 처리)
        if (customPrincipal == null) {
            log.error("인증되지 않은 사용자의 AI 텍스트 생성 요청");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        log.info("AI 텍스트 생성 요청 - userId: {}, prompt: {}", 
                customPrincipal.getUserId(), req.getOrDefault("prompt", "").substring(0, Math.min(50, req.getOrDefault("prompt", "").length())));
        
        try {
            String prompt = req.getOrDefault("prompt", "");
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "프롬프트가 비어있습니다.");
            }
            
            String text = aiService.generateText(prompt);
            log.info("AI 텍스트 생성 완료 - userId: {}", customPrincipal.getUserId());
            
            return ResponseEntity.ok(Map.of("text", text));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 텍스트 생성 중 오류 발생 - userId: {}", customPrincipal.getUserId(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 텍스트 생성 중 오류가 발생했습니다.");
        }
    }
}
