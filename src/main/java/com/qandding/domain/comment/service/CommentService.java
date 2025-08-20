package com.qandding.domain.comment.service;

import com.qandding.domain.answer.entity.AnswerPost;
import com.qandding.domain.answer.repository.AnswerPostRepository;
import com.qandding.domain.comment.dto.CommentDtos;
import com.qandding.domain.comment.entity.Comment;
import com.qandding.domain.comment.entity.CommentImage;
import com.qandding.domain.comment.repository.CommentImageRepository;
import com.qandding.domain.comment.repository.CommentRepository;
import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.qandding.global.storage.S3UploadService;
import com.qandding.global.storage.S3PresignService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentImageRepository commentImageRepository;
    private final AnswerPostRepository answerPostRepository;
    private final UserRepository userRepository;
    private final S3UploadService s3UploadService;
    private final S3PresignService s3PresignService;

    @Transactional
    public Long createComment(Long answerPostId, String content, Long userId) {
        log.debug("createComment() 호출됨 - answerPostId: {}, userId: {}", answerPostId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AnswerPost answer = answerPostRepository.findById(answerPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        log.debug("DB 조회 완료 - user: {}, answer: {}", user.getNickname(), answer.getTitle());

        Comment comment = commentRepository.save(new Comment(answer, user, content));
        log.debug("Comment 저장 완료 - commentId: {}", comment.getId());
        log.debug("createComment() 반환 - commentId: {}", comment.getId());
        return comment.getId();
    }

    @Transactional
    public Long createCommentWithFiles(Long answerPostId, String content, java.util.List<MultipartFile> files, Long userId) {
        log.debug("createCommentWithFiles() 호출됨 - answerPostId: {}, userId: {}", answerPostId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AnswerPost answer = answerPostRepository.findById(answerPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        log.debug("DB 조회 완료 - user: {}, answer: {}", user.getNickname(), answer.getTitle());

        Comment comment = commentRepository.save(new Comment(answer, user, content));
        log.debug("Comment 저장 완료 - commentId: {}", comment.getId());

        if (files != null && !files.isEmpty()) {
            int i = 0;
            for (MultipartFile file : files) {
                try {
                    if (!file.isEmpty()) {
                        log.debug("파일 업로드 시도 - fileName: {}", file.getOriginalFilename());
                        String url = s3UploadService.uploadFile(file); // 이미지/PDF 모두 허용
                        log.debug("파일 업로드 성공 - url: {}", url);
                        commentImageRepository.save(new CommentImage(comment, url, i++));
                        log.debug("CommentImage 저장 완료");
                    }
                } catch (java.io.IOException e) {
                    log.error("Failed to upload comment file: {}", file.getOriginalFilename(), e);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }
            }
            log.debug("파일 {}개 처리 완료", files.size());
        }
        log.debug("createCommentWithFiles() 반환 - commentId: {}", comment.getId());
        return comment.getId();
    }

    @Transactional
    public Long createReplyWithFiles(Long parentCommentId, String content, java.util.List<MultipartFile> files, Long userId) {
        log.debug("createReplyWithFiles() 호출됨 - parentCommentId: {}, userId: {}", parentCommentId, userId);
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        log.debug("부모 댓글 조회 완료 - parentCommentId: {}", parent.getId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        log.debug("사용자 조회 완료 - userId: {}", user.getId());

        Comment reply = commentRepository.save(new Comment(parent.getAnswerPost(), user, content, parent));
        log.debug("대댓글 저장 완료 - replyId: {}", reply.getId());

        if (files != null && !files.isEmpty()) {
            int i = 0;
            for (MultipartFile file : files) {
                try {
                    if (!file.isEmpty()) {
                        log.debug("파일 업로드 시도 - fileName: {}", file.getOriginalFilename());
                        String url = s3UploadService.uploadFile(file);
                        log.debug("파일 업로드 성공 - url: {}", url);
                        commentImageRepository.save(new CommentImage(reply, url, i++));
                        log.debug("CommentImage 저장 완료");
                    }
                } catch (java.io.IOException e) {
                    log.error("Failed to upload reply file: {}", file.getOriginalFilename(), e);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }
            }
            log.debug("파일 {}개 처리 완료", files.size());
        }
        log.debug("createReplyWithFiles() 반환 - replyId: {}", reply.getId());
        return reply.getId();
    }

    public Page<CommentDtos.Thread> listThreads(Long answerPostId, int page, int size) {
        log.debug("listThreads() 호출됨 - answerPostId: {}, page: {}, size: {}", answerPostId, page, size);
        // 일반 커뮤니티 스타일: 상위 댓글도 오래된순(오름차순), 대댓글도 오래된순
        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt"));
        Page<Comment> parents = commentRepository.findByAnswerPost_IdAndParentIsNull(answerPostId, pageable);
        log.debug("상위 댓글 {}개 조회", parents.getNumberOfElements());
        java.util.List<Long> parentIds = parents.map(Comment::getId).toList();

        java.util.Map<Long, java.util.List<String>> imageMap = new java.util.HashMap<>();
        if (!parentIds.isEmpty()) {
            var images = commentImageRepository.findByCommentIdInOrderBySortOrderAsc(parentIds);
            log.debug("상위 댓글 이미지 {}개 조회", images.size());
            for (var ci : images) {
                imageMap.computeIfAbsent(ci.getComment().getId(), k -> new java.util.ArrayList<>())
                        .add(this.s3PresignService.presignGetUrlByUrl(ci.getUrl()));
            }
        }

        var replies = parentIds.isEmpty() ? java.util.List.<Comment>of() : commentRepository.findByParent_IdInOrderByCreatedAtAsc(parentIds);
        log.debug("대댓글 {}개 조회", replies.size());
        java.util.Map<Long, java.util.List<Comment>> repliesMap = new java.util.HashMap<>();
        for (var r : replies) repliesMap.computeIfAbsent(r.getParent().getId(), k -> new java.util.ArrayList<>()).add(r);

        // load images for replies
        if (!replies.isEmpty()) {
            var replyIds = replies.stream().map(Comment::getId).toList();
            var rimgs = commentImageRepository.findByCommentIdInOrderBySortOrderAsc(replyIds);
            log.debug("대댓글 이미지 {}개 조회", rimgs.size());
            java.util.Map<Long, java.util.List<String>> rimgMap = new java.util.HashMap<>();
            for (var ci : rimgs) rimgMap.computeIfAbsent(ci.getComment().getId(), k -> new java.util.ArrayList<>())
                    .add(this.s3PresignService.presignGetUrlByUrl(ci.getUrl()));
            // combine later per reply
            // store in imageMap for convenience as well is fine, but we'll handle separately
            imageMap.putAll(rimgMap);
        }

        java.util.List<CommentDtos.Thread> threads = new java.util.ArrayList<>();
        for (var p : parents.getContent()) {
            var pSummary = new CommentDtos.Summary(p.getId(), p.getUser().getNickname(), p.getContent(), p.getCreatedAt(),
                    imageMap.getOrDefault(p.getId(), java.util.List.of()));
            var child = repliesMap.getOrDefault(p.getId(), java.util.List.of()).stream()
                    .map(c -> new CommentDtos.Summary(c.getId(), c.getUser().getNickname(), c.getContent(), c.getCreatedAt(),
                            imageMap.getOrDefault(c.getId(), java.util.List.of())))
                    .toList();
            threads.add(new CommentDtos.Thread(pSummary, child));
        }
        log.debug("댓글 스레드 {}개 생성", threads.size());
        Page<CommentDtos.Thread> pageResult = new PageImpl<>(threads, pageable, parents.getTotalElements());
        log.debug("listThreads() 반환 - Page: {}/{} (총 {}개)", pageResult.getNumber(), pageResult.getTotalPages(), pageResult.getTotalElements());
        return pageResult;
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.debug("deleteComment() 호출됨 - commentId: {}, userId: {}", commentId, userId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        log.debug("댓글 조회 완료 - commentId: {}", comment.getId());

        if (!comment.getUser().getId().equals(userId)) {
            log.warn("댓글 삭제 권한 없음 - commentId: {}, userId: {}, ownerId: {}", commentId, userId, comment.getUser().getId());
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
        }

        commentRepository.delete(comment);
        log.info("Successfully deleted comment {}", commentId);
        log.debug("deleteComment() 반환 - void");
    }
}
