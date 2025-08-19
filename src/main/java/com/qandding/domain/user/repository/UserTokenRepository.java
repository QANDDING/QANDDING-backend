package com.qandding.domain.user.repository;

import com.qandding.domain.user.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    @Query("SELECT ut FROM UserToken ut WHERE ut.user.id = :userId AND ut.tokenType = :tokenType AND ut.expiresAt > :now")
    Optional<UserToken> findValidTokenByUserIdAndType(@Param("userId") Long userId, @Param("tokenType") UserToken.TokenType tokenType, @Param("now") LocalDateTime now);

    @Query("SELECT ut FROM UserToken ut WHERE ut.accessToken = :accessToken AND ut.expiresAt > :now")
    Optional<UserToken> findValidAccessToken(@Param("accessToken") String accessToken, @Param("now") LocalDateTime now);

    @Query("SELECT ut FROM UserToken ut WHERE ut.refreshToken = :refreshToken AND ut.expiresAt > :now")
    Optional<UserToken> findValidRefreshToken(@Param("refreshToken") String refreshToken, @Param("now") LocalDateTime now);

    @Query("SELECT ut FROM UserToken ut WHERE ut.user.id = :userId AND ut.expiresAt > :now")
    List<UserToken> findAllValidTokensByUserId(@Param("userId") Long userId);

    // 블랙리스트 관련 메서드 제거

    @Modifying
    @Query("DELETE FROM UserToken ut WHERE ut.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(ut) FROM UserToken ut WHERE ut.user.id = :userId AND ut.tokenType = :tokenType AND ut.expiresAt > :now")
    long countValidTokensByUserIdAndType(@Param("userId") Long userId, @Param("tokenType") UserToken.TokenType tokenType, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserToken ut WHERE ut.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
