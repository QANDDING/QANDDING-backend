package com.qandding.domain.ai.listener;

import com.qandding.domain.ai.GeminiClient;
import com.qandding.domain.ai.OcrSolveService;
import com.qandding.domain.ai.event.AiRegenerateRequestedEvent;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.AnswerType;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionImageRepository;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRegenerateListener {
    private final QuestionPostRepository questionPostRepository;
    private final QuestionImageRepository questionImageRepository;
    private final AnswerPostRepository answerPostRepository;
    private final AnswerImageRepository answerImageRepository;
    private final GeminiClient geminiClient;
    private final OcrSolveService ocrSolveService;

    @Async("aiTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegenerateRequested(AiRegenerateRequestedEvent event) {
        Long qid = event.questionPostId();
        log.info("[Async] AI 재생성 이벤트 수신: questionPostId={}", qid);
        try {
            // 쿨다운 재확인
            final int COOLDOWN_SECONDS = 60;
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusSeconds(COOLDOWN_SECONDS);
            AnswerPost latestAiAnswer = answerPostRepository.findTopByQuestionPost_IdAndAnswerTypeOrderByCreatedAtDesc(qid, AnswerType.AI);
            if (latestAiAnswer != null && latestAiAnswer.getCreatedAt().isAfter(cutoff)) {
                log.warn("[Async] 재생성 쿨다운으로 건너뜀: questionPostId={}", qid);
                return;
            }

            QuestionPost q = questionPostRepository.findById(qid)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

            String instruction = buildInstruction(q.getTitle(), q.getContent(), event.prompt());

            String aiResponse;
            List<String> usedImageUrls = new ArrayList<>();
            List<String> eventUrls = event.fileUrls();
            if (eventUrls != null && !eventUrls.isEmpty()) {
                usedImageUrls.addAll(eventUrls);
                aiResponse = ocrSolveService.solveFromUrls(instruction, eventUrls).blockOptional().orElse("");
            } else {
                var qImgs = questionImageRepository.findByQuestionPostIdOrderBySortOrderAsc(qid);
                List<String> qUrls = qImgs.stream().map(img -> img.getUrl()).toList();
                if (!qUrls.isEmpty()) {
                    usedImageUrls.addAll(qUrls);
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
                ? ("AI 답변 - " + q.getTitle())
                : event.title();
            if (title.length() > 200) title = title.substring(0, 197) + "...";

            // Find existing post to update, or create a new one
            AnswerPost postToUpdate = answerPostRepository.findTopByQuestionPost_IdAndAnswerTypeOrderByCreatedAtDesc(qid, AnswerType.AI);
            if (postToUpdate != null) {
                postToUpdate.update(title, aiResponse);
                answerPostRepository.save(postToUpdate);
                log.info("[Async] AI 답변 업데이트 완료: questionPostId={}, answerPostId={}", qid, postToUpdate.getId());
            } else {
                // The event doesn't carry the requester, so we assume the question author is the requester.
                AnswerPost newPost = AnswerPost.createForAi(q, q.getUser(), title, aiResponse);
                answerPostRepository.save(newPost);
                log.info("[Async] AI 답변 생성 완료: questionPostId={}, answerPostId={}", qid, newPost.getId());
            }

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