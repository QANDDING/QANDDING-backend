package com.qandding.domain.user.entity;

import com.qandding.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "user_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    	@Column(name = "refresh_token", nullable = true, length = 1000)
	private String refreshToken;

	@Column(name = "access_token", nullable = true, length = 1000)
	private String accessToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    	@Enumerated(EnumType.STRING)
	@Column(name = "token_type", nullable = false)
	private TokenType tokenType;

    public enum TokenType {
        ACCESS, REFRESH
    }

    public static UserToken createAccessToken(User user, String accessToken, LocalDateTime expiresAt) {
        UserToken userToken = new UserToken();
        userToken.user = user;
        userToken.accessToken = accessToken;
        userToken.refreshToken = null;
        userToken.expiresAt = expiresAt;
        userToken.tokenType = TokenType.ACCESS;
        return userToken;
    }

    public static UserToken createRefreshToken(User user, String refreshToken, LocalDateTime expiresAt) {
        UserToken userToken = new UserToken();
        userToken.user = user;
        userToken.refreshToken = refreshToken;
        userToken.accessToken = null;
        userToken.expiresAt = expiresAt;
        userToken.tokenType = TokenType.REFRESH;
        return userToken;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    	// 블랙리스트 관련 메서드 제거

	@PrePersist
	@PreUpdate
	private void validateTokenConstraints() {
		if (tokenType == TokenType.ACCESS) {
			if (accessToken == null || accessToken.isBlank()) {
				throw new IllegalStateException("ACCESS 토큰 타입일 때는 accessToken이 필수입니다.");
			}
			if (refreshToken != null && !refreshToken.isBlank()) {
				throw new IllegalStateException("ACCESS 토큰 타입일 때는 refreshToken이 null이어야 합니다.");
			}
		} else if (tokenType == TokenType.REFRESH) {
			if (refreshToken == null || refreshToken.isBlank()) {
				throw new IllegalStateException("REFRESH 토큰 타입일 때는 refreshToken이 필수입니다.");
			}
			if (accessToken != null && !accessToken.isBlank()) {
				throw new IllegalStateException("REFRESH 토큰 타입일 때는 accessToken이 null이어야 합니다.");
			}
		}
	}
}
