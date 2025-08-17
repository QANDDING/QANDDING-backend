package com.qandding.comment.service;

import com.qandding.common.error.BusinessException;
import com.qandding.common.error.ErrorCode;
import com.qandding.answer.domain.AnswerPost;
import com.qandding.answer.repository.AnswerPostRepository;
import com.qandding.comment.domain.Comment;
import com.qandding.comment.domain.CommentImage;
import com.qandding.comment.presentation.dto.CommentDtos;
import com.qandding.comment.repository.CommentImageRepository;
import com.qandding.comment.repository.CommentQueryRepository;
import com.qandding.comment.repository.CommentRepository;
import com.qandding.user.domain.User;
import com.qandding.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {
	private final CommentRepository commentRepository;
	private final CommentImageRepository commentImageRepository;
	private final CommentQueryRepository commentQueryRepository;
	private final AnswerPostRepository answerPostRepository;
	private final UserRepository userRepository;

	public CommentService(CommentRepository commentRepository,
	                     CommentImageRepository commentImageRepository,
	                     CommentQueryRepository commentQueryRepository,
	                     AnswerPostRepository answerPostRepository,
	                     UserRepository userRepository) {
		this.commentRepository = commentRepository;
		this.commentImageRepository = commentImageRepository;
		this.commentQueryRepository = commentQueryRepository;
		this.answerPostRepository = answerPostRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public Long createComment(Long userId, Long answerPostId, String content, List<String> imageUrls) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		AnswerPost answer = answerPostRepository.findById(answerPostId).orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
		Comment comment = commentRepository.save(new Comment(answer, user, content));
		if (imageUrls != null) {
			int i = 0;
			for (String url : imageUrls) {
				commentImageRepository.save(new CommentImage(comment, url, i++));
			}
		}
		return comment.getId();
	}

	public Page<CommentDtos.Summary> listComments(Long answerPostId, Pageable pageable) {
		return commentQueryRepository.findSummaries(answerPostId, pageable);
	}

	@Transactional
	public void deleteComment(Long principalUserId, Long id) {
		Comment comment = commentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
		if (!comment.getUser().getId().equals(principalUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION);
		}
		commentRepository.delete(comment);
	}
}



