package com.qandding.domain.ai.service;

import com.qandding.domain.ai.GeminiClient;
import com.qandding.domain.ai.OcrSolveService;
import com.qandding.domain.ai.dto.AiAnswerDtos;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.entity.AnswerType;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiAnswerService {

    private final QuestionPostRepository questionPostRepository;
    private final AnswerPostRepository answerPostRepository;
    private final UserRepository userRepository;
    private final OcrSolveService ocrSolveService;
    private final GeminiClient geminiClient;

    @Transactional
    public AiAnswerDtos.Detail generateAndSave(Long questionPostId, String prompt, String title, Long userId) {
        User requester = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost questionPost = questionPostRepository.findById(questionPostId).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String aiResponse = geminiClient.generateText(prompt).blockOptional().orElse("");
        if (aiResponse == null || aiResponse.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        if (aiResponse.length() > 65000) {
            aiResponse = aiResponse.substring(0, 65000) + "\n\n[내용이 너무 길어 일부가 생략되었습니다.]";
        }

        String finalTitle = (title == null || title.isBlank()) ? ("AI 답변 - " + questionPost.getTitle()) : title;
        if (finalTitle.length() > 200) finalTitle = finalTitle.substring(0, 197) + "...";

        // Create AnswerPost with AI type
        AnswerPost answerPost = AnswerPost.createForAi(questionPost, requester, finalTitle, aiResponse);
        answerPostRepository.save(answerPost);

        return new AiAnswerDtos.Detail(answerPost.getId(), answerPost.getTitle(), answerPost.getContent(), answerPost.getCreatedAt(), List.of());
    }

    @Transactional
    public AiAnswerDtos.Detail generateOrReplace(Long questionPostId, String prompt, String title, MultipartFile file, Long userId) throws IOException {
        User requester = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost questionPost = questionPostRepository.findById(questionPostId).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String aiResponse;
        if (file != null && !file.isEmpty()) {
            aiResponse = ocrSolveService.solveFromUpload(prompt != null ? prompt : questionPost.getTitle(), file).blockOptional().orElse("");
        } else {
            String p = (prompt == null || prompt.isBlank()) ? ("질문: " + questionPost.getTitle() + "\n" + questionPost.getContent()) : prompt;
            aiResponse = geminiClient.generateText(p).blockOptional().orElse("");
        }
        if (aiResponse == null || aiResponse.isEmpty()) throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        if (aiResponse.length() > 65000) aiResponse = aiResponse.substring(0, 65000) + "\n\n[내용이 너무 길어 일부가 생략되었습니다.]";

        String finalTitle = (title == null || title.isBlank()) ? ("AI 답변 - " + questionPost.getTitle()) : title;
        if (finalTitle.length() > 200) finalTitle = finalTitle.substring(0, 197) + "...";

        AnswerPost existing = answerPostRepository.findTopByQuestionPost_IdAndAnswerTypeOrderByCreatedAtDesc(questionPostId, AnswerType.AI);
        
        if (existing != null) {
            existing.update(finalTitle, aiResponse);
            answerPostRepository.save(existing);
            return new AiAnswerDtos.Detail(existing.getId(), existing.getTitle(), existing.getContent(), existing.getCreatedAt(), java.util.List.of());
        }

        AnswerPost answerPost = AnswerPost.createForAi(questionPost, requester, finalTitle, aiResponse);
        answerPostRepository.save(answerPost);
        return new AiAnswerDtos.Detail(answerPost.getId(), answerPost.getTitle(), answerPost.getContent(), answerPost.getCreatedAt(), java.util.List.of());
    }
}