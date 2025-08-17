package com.qandding.domain.ai;

import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HuggingFaceClient {
  private final WebClient webClient;
  private final String model;
  private final String apiKey;
  private final GeminiClient geminiClient;

  public HuggingFaceClient(@Value("${app.huggingface.api-key}") String apiKey,
      @Value("${app.huggingface.model:Salesforce/blip-vqa-base}") String model,
      GeminiClient geminiClient) {
    this.apiKey = apiKey;
    this.model = model;
    this.geminiClient = geminiClient;
    this.webClient = WebClient.builder()
        .baseUrl("https://api-inference.huggingface.co")
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .build();
  }

  /**
   * 텍스트만으로 AI 응답 생성 (Gemini 사용)
   */
  public Mono<String> generateText(String prompt) {
    log.info("Generating text response using Gemini for prompt: {}", prompt);
    return geminiClient.generateText(prompt);
  }

  /**
   * 이미지와 텍스트를 함께 사용하여 AI 응답 생성
   */
  public Mono<String> generateTextWithImage(String prompt, String imagePath) {
    try {
      byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
      return generateTextWithImageBytes(prompt, imageBytes);
    } catch (IOException e) {
      return Mono.error(new RuntimeException("이미지 파일을 읽을 수 없습니다: " + imagePath, e));
    }
  }

  /**
   * 이미지 바이트 배열과 텍스트를 함께 사용하여 AI 응답 생성
   */
  public Mono<String> generateTextWithImageBytes(String prompt, byte[] imageBytes) {
    log.info("Generating AI response with image using HuggingFace model: {}", model);
    log.info("Image size: {} bytes, prompt: {}", imageBytes.length, prompt);
    
    // 이미지를 Base64로 인코딩
    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
    log.info("Base64 image length: {}", base64Image.length());
    
    // microsoft/git-base-coco 모델용 요청 형식
    Map<String, Object> body = Map.of(
      "inputs", Map.of(
        "image", "data:image/jpeg;base64," + base64Image,
        "text", prompt
      )
    );
    
    log.info("Sending request to HuggingFace model: {}", model);
    log.info("Request body keys: {}", body.keySet());
    
    return webClient
      .post()
      .uri("/models/" + model)
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(body)
      .retrieve()
      .bodyToMono(String.class)
      .map(response -> {
        try {
          log.info("Raw HuggingFace response: {}", response);
          
          // JSON 응답에서 텍스트 추출
          if (response.contains("\"generated_text\"")) {
            int start = response.indexOf("\"generated_text\":\"") + 17;
            int end = response.indexOf("\"", start);
            if (start > 16 && end > start) {
              String result = response.substring(start, end);
              log.info("Extracted generated_text: {}", result);
              
              // 프롬프트와 이미지 설명을 결합하여 최종 응답 생성
              String finalResponse = String.format("이미지 분석 결과: %s\n\n질문: %s\n\n답변: %s", 
                result, prompt, result);
              return finalResponse;
            }
          }
          
          // 응답이 예상과 다르면 원본 반환
          log.warn("Unexpected response format, returning raw response");
          return response;
          
        } catch (Exception e) {
          log.error("Error parsing HuggingFace response: {}", e.getMessage());
          return "이미지 분석 결과를 처리하는 중 오류가 발생했습니다.";
        }
      })
      .onErrorResume(e -> {
        // 이미지 인식 실패 시 Gemini로 텍스트만 응답 생성
        log.warn("이미지 인식 실패, Gemini로 텍스트만 응답 생성: {}", e.getMessage());
        log.warn("Error details: {}", e.getClass().getSimpleName());
        return generateText(prompt);
      });
  }
}
