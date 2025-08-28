package com.qandding.domain.user.repository;

import com.qandding.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserPostQueryRepository extends JpaRepository<User, Long> {

    // Native Query 결과를 매핑할 Projection 인터페이스 정의
    interface UnifiedPostProjection {
        String getPostType();
        Long getPostId();
        String getTitle();
        LocalDateTime getCreatedAt();
        Long getOriginalQuestionId();
    }

    // Native Query를 사용하여 질문과 답변을 통합 조회
    @Query(value = """
        (SELECT 
            'QUESTION' as postType, 
            q.question_post_id as postId, 
            q.title as title, 
            q.created_at as createdAt, 
            NULL as originalQuestionId 
        FROM question_post q 
        WHERE q.user_id = :userId 
        AND (:keyword IS NULL OR :keyword = '' OR q.title LIKE CONCAT('%', :keyword, '%')) 
        AND (:postType IS NULL OR :postType = '' OR :postType = 'QUESTION')) 
        UNION ALL 
        (SELECT 
            'ANSWER' as postType, 
            a.answer_post_id as postId, 
            a.title as title, 
            a.created_at as createdAt, 
            a.question_post_id as originalQuestionId 
        FROM answer_post a 
        WHERE a.author_id = :userId 
        AND (:keyword IS NULL OR :keyword = '' OR a.title LIKE CONCAT('%', :keyword, '%')) 
        AND (:postType IS NULL OR :postType = '' OR :postType = 'ANSWER')) 
        ORDER BY createdAt DESC""",
        countQuery = """
        SELECT COUNT(*) FROM (
            (SELECT q.question_post_id FROM question_post q WHERE q.user_id = :userId AND (:keyword IS NULL OR :keyword = '' OR q.title LIKE CONCAT('%', :keyword, '%')) AND (:postType IS NULL OR :postType = '' OR :postType = 'QUESTION')) 
            UNION ALL 
            (SELECT a.answer_post_id FROM answer_post a WHERE a.author_id = :userId AND (:keyword IS NULL OR :keyword = '' OR a.title LIKE CONCAT('%', :keyword, '%')) AND (:postType IS NULL OR :postType = '' OR :postType = 'ANSWER'))
        ) as combined_posts""",
        nativeQuery = true)
    Page<UnifiedPostProjection> findUnifiedPostsByUserId(@Param("userId") Long userId, Pageable pageable, @Param("keyword") String keyword, @Param("postType") String postType);
}
