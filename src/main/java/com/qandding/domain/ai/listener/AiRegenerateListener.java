package com.qandding.domain.ai.listener;

import com.qandding.domain.ai.GeminiClient;
import com.qandding.domain.ai.OcrSolveService;
import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.ai.event.AiRegenerateRequestedEvent;
import com.qandding.domain.ai.repository.AiAnswerRepository;
import com.qandding.domain.answer.entity.AnswerImage;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionImageRepository;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRegenerateListener {
    private final QuestionPostRepository questionPostRepository;
    private final QuestionImageRepository questionImageRepository;
    private final AiAnswerRepository aiAnswerRepository;
    private final AnswerPostRepository answerPostRepository;
    private final AnswerImageRepository answerImageRepository;
    private final GeminiClient geminiClient;
    private final OcrSolveService ocrSolveService;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegenerateRequested(AiRegenerateRequestedEvent event) {
        Long qid = event.questionPostId();
        log.info("[Async] AI 재생성 이벤트 수신: questionPostId={}, fileUrls={}", qid, event.fileUrls() != null ? event.fileUrls().size() : 0);
        try {
            // 쿨다운 재확인
            final int COOLDOWN_SECONDS = 60;
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusSeconds(COOLDOWN_SECONDS);
            boolean cooling = answerPostRepository.existsByQuestionPost_IdAndAiAnswerIsNotNullAndCreatedAtAfter(qid, cutoff);
            if (cooling) {
                log.warn("[Async] 재생성 쿨다운으로 건너뜀: questionPostId={}", qid);
                return;
            }

            QuestionPost q = questionPostRepository.findById(qid)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

            String instruction = buildInstruction(q.getTitle(), q.getContent(), event.prompt());

            String aiResponse;
            List<String> used = new ArrayList<>();
            List<String> urls = event.fileUrls();
            if (urls != null && !urls.isEmpty()) {
                used.addAll(urls);
                aiResponse = ocrSolveService.solveFromUrls(instruction, urls).blockOptional().orElse("");
            } else {
                var qImgs = questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(qid);
                List<String> qUrls = qImgs.stream().map(img -> img.getUrl()).toList();
                if (!qUrls.isEmpty()) {
                    used.addAll(qUrls);
                    aiResponse = ocrSolveService.solveFromUrls(instruction, qUrls).blockOptional().orElse("");
                } else {
                    aiResponse = geminiClient.generateText(instruction).blockOptional().orElse("");
                }
            }
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("[Async] 재생성 결과 비어있음: questionPostId={}", qid);
                return;
            }

            if (aiResponse.length() > 65000) {
                aiResponse = aiResponse.substring(0, 65000) + "\n\n[내용이 일부 생략됨]";
            }
            String title = (event.title() == null || event.title().isBlank())
                ? ("AI 재생성 - " + q.getTitle())
                : event.title();
            if (title.length() > 200) title = title.substring(0, 197) + "...";

            AiAnswer ai = aiAnswerRepository.save(new AiAnswer(title, aiResponse));
            AnswerPost ap = answerPostRepository.save(new AnswerPost(q.getUser(), q, ai, title, aiResponse));
            for (int i = 0; i < used.size(); i++) {
                answerImageRepository.save(new AnswerImage(ap, used.get(i), i));
            }
            log.info("[Async] 재생성 완료: questionPostId={}, answerPostId={}", qid, ap.getId());
        } catch (Exception e) {
            log.error("[Async] 재생성 실패: {}", e.getMessage(), e);
        }
    }

    private String buildInstruction(String title, String content, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("질문 제목: ").append(title == null ? "" : title).append('\n');
        sb.append("질문 내용: ").append(content == null ? "" : content).append('\n');
        if (extra != null && !extra.isBlank()) sb.append("보정 프롬프트: ").append(extra).append('\n');
        sb.append("위 내용을 바탕으로 풀이 과정을 단계별로 전개하고 마지막에 정답을 제시하세요.");
        return sb.toString();
    }
}
