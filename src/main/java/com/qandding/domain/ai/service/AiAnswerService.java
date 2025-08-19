package com.qandding.domain.ai.service;

import com.qandding.domain.ai.GeminiClient;
import com.qandding.domain.ai.OcrSolveService;
import com.qandding.domain.ai.dto.AiAnswerDtos;
import com.qandding.domain.ai.entity.AiAnswer;
import com.qandding.domain.ai.repository.AiAnswerRepository;
import com.qandding.domain.answer.entity.AnswerImage;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import com.qandding.global.storage.S3PresignService;
import com.qandding.global.storage.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AiAnswerService {

    private final AiAnswerRepository aiAnswerRepository;
    private final QuestionPostRepository questionPostRepository;
    private final AnswerPostRepository answerPostRepository;
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final AnswerImageRepository answerImageRepository;
    private final S3PresignService s3PresignService;
    private final OcrSolveService ocrSolveService;
    private final GeminiClient geminiClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long create(String title, String content, Long questionPostId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost questionPost = questionPostRepository.findById(questionPostId).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        AiAnswer aiAnswer = aiAnswerRepository.save(new AiAnswer(title, content));
        AnswerPost answerPost = answerPostRepository.save(new AnswerPost(user, questionPost, aiAnswer, title, content));
        return answerPost.getId();
    }

    @Transactional
    public AiAnswerDtos.Detail generateAndSaveWithOcr(Long questionPostId, String question, String title, MultipartFile file, Long userId) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost questionPost = questionPostRepository.findById(questionPostId).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // AI 답변은 사람 첨부 이미지 저장 대상이 아닙니다. S3 업로드 없이 바로 OCR 처리만 수행합니다.

        String aiResponse = ocrSolveService.solveFromUpload(question, file).blockOptional().orElse("");
        if (aiResponse == null || aiResponse.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        if (aiResponse.length() > 65000) {
            aiResponse = aiResponse.substring(0, 65000) + "\n\n[내용이 너무 길어 일부가 생략되었습니다.]";
        }

        String finalTitle = (title == null || title.isBlank()) ? ("OCR AI 답변 - " + questionPost.getTitle()) : title;
        if (finalTitle.length() > 200) finalTitle = finalTitle.substring(0, 197) + "...";

        AiAnswer aiAnswer = aiAnswerRepository.save(new AiAnswer(finalTitle, aiResponse));
        AnswerPost answerPost = answerPostRepository.save(new AnswerPost(user, questionPost, aiAnswer, finalTitle, aiResponse));
        // 이미지 링크는 저장/반환하지 않습니다(사람이 올린 이미지가 아님)
        return new AiAnswerDtos.Detail(aiAnswer.getId(), aiAnswer.getTitle(), aiResponse, aiAnswer.getCreatedAt(), List.of());
    }

    @Transactional
    public AiAnswerDtos.Detail generateAndSave(Long questionPostId, String prompt, String title, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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

        AiAnswer aiAnswer = aiAnswerRepository.save(new AiAnswer(finalTitle, aiResponse));
        answerPostRepository.save(new AnswerPost(user, questionPost, aiAnswer, finalTitle, aiResponse));

        return AiAnswerDtos.Detail.from(aiAnswer);
    }

    @Transactional
    public AiAnswerDtos.Detail generateAndSaveWithImage(Long questionPostId, String prompt, String title, MultipartFile image, Long userId) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuestionPost questionPost = questionPostRepository.findById(questionPostId).orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String aiResponse;
        if (image != null && !image.isEmpty()) {
            // 업로드 없이 OCR만 수행
            aiResponse = ocrSolveService.solveFromUpload(prompt, image).blockOptional().orElse("");
        } else {
            aiResponse = geminiClient.generateText(prompt).blockOptional().orElse("");
        }

        if (aiResponse == null || aiResponse.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        if (aiResponse.length() > 65000) {
            aiResponse = aiResponse.substring(0, 65000) + "\n\n[내용이 너무 길어 일부가 생략되었습니다.]";
        }

        String finalTitle = (title == null || title.isBlank()) ? ("AI 답변 - " + questionPost.getTitle()) : title;
        if (finalTitle.length() > 200) finalTitle = finalTitle.substring(0, 197) + "...";

        AiAnswer aiAnswer = aiAnswerRepository.save(new AiAnswer(finalTitle, aiResponse));
        AnswerPost answerPost = answerPostRepository.save(new AnswerPost(user, questionPost, aiAnswer, finalTitle, aiResponse));

        // 이미지 링크는 저장/반환하지 않습니다(사람이 올린 이미지가 아님)
        return new AiAnswerDtos.Detail(aiAnswer.getId(), aiAnswer.getTitle(), aiResponse, aiAnswer.getCreatedAt(), List.of());
    }

    @Transactional
    public AiAnswerDtos.Detail getAiAnswerDetail(Long id) {
        AiAnswer answer = aiAnswerRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        List<String> urls = new ArrayList<>();
        var ap = answerPostRepository.findByAiAnswer_Id(id);
        if (ap != null) {
            var imgs = answerImageRepository.findByAnswerPostIdOrderBySortOrderAsc(ap.getId());
            for (var ai : imgs) {
                urls.add(s3PresignService.presignGetUrlByUrl(ai.getUrl()));
            }
        }
        return new AiAnswerDtos.Detail(answer.getId(), answer.getTitle(), answer.getContent(), answer.getCreatedAt(), urls);
    }

    @Transactional
    public AiAnswerDtos.Detail generateOrReplace(Long questionPostId, String prompt, String title, MultipartFile file, Long userId) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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

        // 기존 AI 답변이 있으면 대체, 없으면 생성
        AnswerPost existing = answerPostRepository.findTopByQuestionPost_IdAndAiAnswerIsNotNullOrderByCreatedAtDesc(questionPostId);
        if (existing != null && existing.getAiAnswer() != null) {
            AiAnswer ai = existing.getAiAnswer();
            ai.update(finalTitle, aiResponse);
            existing.update(finalTitle, aiResponse);
            return new AiAnswerDtos.Detail(ai.getId(), finalTitle, aiResponse, ai.getCreatedAt(), java.util.List.of());
        }

        AiAnswer aiAnswer = aiAnswerRepository.save(new AiAnswer(finalTitle, aiResponse));
        answerPostRepository.save(new AnswerPost(user, questionPost, aiAnswer, finalTitle, aiResponse));
        return new AiAnswerDtos.Detail(aiAnswer.getId(), finalTitle, aiResponse, aiAnswer.getCreatedAt(), java.util.List.of());
    }
}
