package com.qandding.domain.question.listener;

import com.qandding.domain.ai.GeminiClient;
import com.qandding.domain.ai.OcrSolveService;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.AnswerType;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.event.QuestionCreatedEvent;
import com.qandding.domain.question.repository.QuestionImageRepository;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.storage.S3PresignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionAiGenerateListener {

    private final QuestionPostRepository questionPostRepository;
    private final QuestionImageRepository questionImageRepository;
    private final AnswerPostRepository answerPostRepository;
    private final GeminiClient geminiClient;
    private final OcrSolveService ocrSolveService;
    private final S3PresignService s3PresignService;

    @Value("${app.ai.auto-generate.enabled:true}")
    private boolean autoGenerateEnabled;

    @Value("${app.ai.auto-generate.cooldown-seconds:600}")
    private int cooldownSeconds;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuestionCreated(QuestionCreatedEvent event) {
        Long qid = event.questionPostId();
        log.info("[Async] 질문 생성 이벤트 수신: questionPostId={}", qid);

        if (!autoGenerateEnabled) {
            log.info("[Async] 자동 AI 생성 비활성화됨. 건너뜀: questionPostId={}", qid);
            return;
        }
        try {
            QuestionPost q = questionPostRepository.findById(qid)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusSeconds(cooldownSeconds);
            AnswerPost latestAiAnswer = answerPostRepository.findTopByQuestionPost_IdAndAnswerTypeOrderByCreatedAtDesc(qid, AnswerType.AI);
            if (latestAiAnswer != null && latestAiAnswer.getCreatedAt().isAfter(cutoff)) {
                log.info("[Async] 최근 AI 답변 존재, 스킵: questionPostId={}, cutoff={}", qid, cutoff);
                return;
            }

            List<String> imageUrls = questionImageRepository
                .findByQuestionPostIdOrderBySortOrderAsc(qid)
                .stream().map(qi -> qi.getUrl()).toList();

            List<String> presignedUrls = new ArrayList<>();
            for (String u : imageUrls) {
                try {
                    presignedUrls.add(s3PresignService.presignGetUrlByUrl(u));
                } catch (Exception e) {
                    log.warn("presign 실패, 건너뜀: {} - {}", u, e.getMessage());
                }
            }

            String prompt = buildPrompt(q.getTitle(), q.getContent());
            String aiResponse;
            if (!presignedUrls.isEmpty()) {
                aiResponse = ocrSolveService.solveFromUrls(prompt, presignedUrls).blockOptional().orElse("");
            } else {
                aiResponse = geminiClient.generateText(prompt).blockOptional().orElse("");
            }
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("[Async] AI 응답이 비어 생성 생략: questionPostId={}", qid);
                return;
            }

            if (aiResponse.length() > 65000) aiResponse = aiResponse.substring(0, 65000) + "\n\n[내용이 일부 생략됨]";
            String aiTitle = (presignedUrls.isEmpty() ? "AI 답변 - " : "OCR AI 답변 - ") + q.getTitle();
            if (aiTitle.length() > 200) aiTitle = aiTitle.substring(0, 197) + "...";

            // Create a new AnswerPost for the AI answer
            AnswerPost answerPost = AnswerPost.createForAi(q, q.getUser(), aiTitle, aiResponse);
            answerPostRepository.save(answerPost);

            log.info("[Async] 질문 기반 AI 답변 생성 완료: questionPostId={}, answerPostId={}", qid, answerPost.getId());
        } catch (Exception e) {
            if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException w && w.getStatusCode().value() == 429) {
                log.warn("[Async] AI 레이트리밋(429)으로 생성 스킵: questionPostId={}", qid);
            } else {
                log.error("[Async] 질문 기반 AI 생성 실패: {}", e.getMessage(), e);
            }
        }
    }

    private String buildPrompt(String title, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("질문 제목: ").append(title == null ? "" : title).append('\n');
        sb.append("질문 내용: ").append(content == null ? "" : content).append('\n');
        sb.append("위 내용을 바탕으로 풀이 과정을 단계별로 전개하고 마지막에 정답을 제시하세요.");
        return sb.toString();
    }
}