package com.qandding.domain.question.listener;

import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.ai.repository.AiAnswerRepository;
import com.qandding.domain.ai.GeminiClient;
import com.qandding.domain.ai.OcrSolveService;
import com.qandding.domain.answer.entity.AnswerImage;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.event.QuestionCreatedEvent;
import com.qandding.domain.question.repository.QuestionImageRepository;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.storage.S3PresignService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionAiGenerateListener {

    private final QuestionPostRepository questionPostRepository;
    private final QuestionImageRepository questionImageRepository;
    private final UserRepository userRepository;
    private final AiAnswerRepository aiAnswerRepository;
    private final AnswerPostRepository answerPostRepository;
    private final AnswerImageRepository answerImageRepository;
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
        log.debug("onQuestionCreated() 호출됨 - event: {}", event);

        if (!autoGenerateEnabled) {
            log.info("[Async] 자동 AI 생성 비활성화됨. 건너뜀: questionPostId={}", qid);
            return;
        }
        try {
            log.debug("질문 ID로 질문 조회 시도: {}", qid);
            QuestionPost q = questionPostRepository.findById(qid)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
            log.debug("질문 조회 성공: {}", q.getTitle());

            // 최근에 생성된 AI 답변이 있으면 스킵하여 레이트리밋 회피
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusSeconds(cooldownSeconds);
            log.debug("AI 답변 생성 쿨다운 확인 - cutoff: {}", cutoff);
            boolean existsRecent = answerPostRepository.existsByQuestionPost_IdAndAiAnswerIsNotNullAndCreatedAtAfter(qid, cutoff);
            if (existsRecent) {
                log.info("[Async] 최근 AI 답변 존재, 스킵: questionPostId={}, cutoff={}", qid, cutoff);
                return;
            }
            log.debug("최근 AI 답변 없음, 생성 계속");

            // 질문 이미지 수집 (원본 S3 URL)
            List<String> imageUrls = questionImageRepository
                .findByQuestionPostIdOrderBySortOrderAsc(qid)
                .stream().map(qi -> qi.getUrl()).toList();
            log.debug("질문 이미지 URL {}개 수집", imageUrls.size());

            // OCR 다운로드용 presigned URL로 변환 (버킷이 private 이므로 필수)
            List<String> presignedUrls = new ArrayList<>();
            for (String u : imageUrls) {
                try {
                    log.debug("Presigning URL for image: {}", u);
                    presignedUrls.add(s3PresignService.presignGetUrlByUrl(u));
                } catch (Exception e) {
                    log.warn("presign 실패, 건너뜀: {} - {}", u, e.getMessage());
                }
            }
            log.debug("Presigned URL {}개 생성", presignedUrls.size());

            String prompt = buildPrompt(q.getTitle(), q.getContent());
            log.debug("생성된 프롬프트: {}", prompt);
            String aiResponse;
            if (!presignedUrls.isEmpty()) {
                log.debug("OCR 기반 AI 답변 생성 시도");
                aiResponse = ocrSolveService.solveFromUrls(prompt, presignedUrls).blockOptional().orElse("");
            } else {
                log.debug("텍스트 기반 AI 답변 생성 시도");
                aiResponse = geminiClient.generateText(prompt).blockOptional().orElse("");
            }
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("[Async] AI 응답이 비어 생성 생략: questionPostId={}", qid);
                return;
            }
            log.debug("AI 응답 수신 (일부): {}", aiResponse.substring(0, Math.min(aiResponse.length(), 100)));

            if (aiResponse.length() > 65000) aiResponse = aiResponse.substring(0, 65000) + "\n\n[내용이 일부 생략됨]";
            String aiTitle = (presignedUrls.isEmpty() ? "AI 답변 - " : "OCR AI 답변 - ") + q.getTitle();
            if (aiTitle.length() > 200) aiTitle = aiTitle.substring(0, 197) + "...";
            log.debug("AI 답변 제목: {}", aiTitle);

            AiAnswer ai = aiAnswerRepository.save(new AiAnswer(aiTitle, aiResponse));
            log.debug("AiAnswer 저장 완료 - aiAnswerId: {}", ai.getId());
            AnswerPost answerPost = answerPostRepository.save(new AnswerPost(q.getUser(), q, ai, aiTitle, aiResponse));
            log.debug("AnswerPost 저장 완료 - answerPostId: {}", answerPost.getId());

            // 주의: AnswerImage는 사용자 답변 전용으로 사용합니다. (AI 답변에는 연결하지 않음)

            log.info("[Async] 질문 기반 AI 답변 생성 완료: questionPostId={}, answerPostId={}", qid, answerPost.getId());
        } catch (Exception e) {
            if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException w && w.getStatusCode().value() == 429) {
                log.warn("[Async] AI 레이트리밋(429)으로 생성 스킵: questionPostId={}", qid);
            } else {
                log.error("[Async] 질문 기반 AI 생성 실패: {}", e.getMessage(), e);
            }
        }
        log.debug("onQuestionCreated() 종료 - questionPostId: {}", event.questionPostId());

    private String buildPrompt(String title, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("질문 제목: ").append(title == null ? "" : title).append('\n');
        sb.append("질문 내용: ").append(content == null ? "" : content).append('\n');
        sb.append("위 내용을 바탕으로 풀이 과정을 단계별로 전개하고 마지막에 정답을 제시하세요.");
        return sb.toString();
    }
}
