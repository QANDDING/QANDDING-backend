package com.qandding.domain.answer.service;

import com.qandding.domain.answer.dto.AnswerDtos;
import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerImageRepository;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.global.common.paging.PageResponse;
import com.qandding.domain.answer.repository.AnswerSelectionRepository;
import com.qandding.domain.question.entity.QuestionPost;
import com.qandding.domain.question.repository.QuestionPostRepository;
import com.qandding.global.storage.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerFeedService {

    private final AnswerPostRepository answerPostRepository;
    private final AnswerImageRepository answerImageRepository;
    private final S3PresignService s3PresignService;
    private final AnswerSelectionRepository answerSelectionRepository;
    private final QuestionPostRepository questionPostRepository;

    public AnswerDtos.Combined getCombinedFeed(Long questionPostId, int page, int size) {
        // 0) 채택된 답변 id 조회
        QuestionPost question = questionPostRepository.findById(questionPostId).orElse(null);
        final Long acceptedAnswerId = (question == null)
                ? null
                : answerSelectionRepository.findByQuestionPost(question)
                    .map(s -> s.getAnswerPost().getId())
                    .orElse(null);

        // 1) AI 답변 (있으면 1개)
        AnswerDtos.Ai aiDetail = null;
        AnswerPost aiPost = answerPostRepository.findFirstByQuestionPost_IdAndAiAnswerIsNotNull(questionPostId);
        if (aiPost != null) {
            aiDetail = new AnswerDtos.Ai(
                    aiPost.getId(), aiPost.getTitle(), aiPost.getContent(),
                    aiPost.getUser().getNickname(), aiPost.getCreatedAt()
            );
        }

        // 2) 사용자 답변 페이지 (AI 제외)
        // 페이징은 id 오름차순으로 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<AnswerPost> userPage = answerPostRepository.findByQuestionPost_IdAndAiAnswerIsNull(questionPostId, pageable);

        // 이미지 일괄 로딩 + 프리사인
        List<Long> ids = userPage.getContent().stream().map(AnswerPost::getId).toList();
        Map<Long, List<String>> imageMap = new HashMap<>();
        if (!ids.isEmpty()) {
            var imgs = answerImageRepository.findByAnswerPostIdInOrderBySortOrderAsc(ids);
            for (var ai : imgs) {
                imageMap.computeIfAbsent(ai.getAnswerPost().getId(), k -> new ArrayList<>())
                        .add(s3PresignService.presignGetUrlByUrl(ai.getUrl()));
            }
        }

        List<AnswerDtos.Detail> userDetails = userPage.getContent().stream().map(p ->
                new AnswerDtos.Detail(
                        p.getId(), p.getTitle(), p.getContent(), p.getUser().getNickname(), p.getCreatedAt(),
                        imageMap.getOrDefault(p.getId(), java.util.List.of()),
                        java.util.Objects.equals(p.getId(), acceptedAnswerId)
                )
        ).toList();

        // If there is a selected answer but it's not in the current page, include it at the top.
        if (acceptedAnswerId != null && userDetails.stream().noneMatch(d -> d.isAccepted())) {
            var acceptedOpt = answerPostRepository.findById(acceptedAnswerId);
            if (acceptedOpt.isPresent()) {
                AnswerPost ap = acceptedOpt.get();
                if (ap.getAiAnswer() == null) { // only user answers
                    List<String> accImgs = answerImageRepository.findByAnswerPostIdOrderBySortOrderAsc(ap.getId())
                            .stream().map(ai -> s3PresignService.presignGetUrlByUrl(ai.getUrl())).toList();
                    AnswerDtos.Detail acceptedDetail = new AnswerDtos.Detail(
                            ap.getId(), ap.getTitle(), ap.getContent(), ap.getUser().getNickname(), ap.getCreatedAt(),
                            accImgs, true
                    );
                    List<AnswerDtos.Detail> adjusted = new ArrayList<>();
                    adjusted.add(acceptedDetail);
                    adjusted.addAll(userDetails);
                    if (adjusted.size() > userPage.getSize()) {
                        adjusted = adjusted.subList(0, userPage.getSize());
                    }
                    userDetails = adjusted;
                }
            }
        }

        Page<AnswerDtos.Detail> detailPage = new PageImpl<>(userDetails, pageable, userPage.getTotalElements());
        PageResponse<AnswerDtos.Detail> users = PageResponse.of(detailPage);

        return new AnswerDtos.Combined(aiDetail, users);
    }
}
