package com.qandding.domain.user.entity;

import com.qandding.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", indexes = {
	@Index(name = "uk_users_email", columnList = "email", unique = true),
	@Index(name = "uk_users_nickname", columnList = "nickname", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(nullable = false, length = 20)
	private String grade;

	@Column(nullable = false, length = 50)
	private String major;

	@Column(nullable = false, length = 120, unique = true)
	private String email;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	public User(String nickname, String grade, String major, String email) {
		this.nickname = nickname;
		this.grade = grade;
		this.major = major;
		this.email = email;
		this.emailVerified = false;
	}

	public Long getId() {
		return this.id;
	}

	public void markEmailVerified() {
		this.emailVerified = true;
	}

	public void updateProfile(String nickname, String grade, String major) {
		if (nickname == null || nickname.isBlank()) throw new IllegalArgumentException("nickname");
		if (grade == null || grade.isBlank()) throw new IllegalArgumentException("grade");
		if (major == null || major.isBlank()) throw new IllegalArgumentException("major");
		this.nickname = nickname;
		this.grade = grade;
		this.major = major;
	}

	public void updateProfile(String nickname, String grade, String major, String email) {
		updateProfile(nickname, grade, major);
		if (email == null || email.isBlank()) throw new IllegalArgumentException("email");
		if (!email.equalsIgnoreCase(this.email)) {
			this.email = email;
			this.emailVerified = false; // 이메일 변경 시 인증 상태 초기화
		}
	}
}
