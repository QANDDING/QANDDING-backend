package com.qandding.domain.user.service;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

    // 수동 회원가입 생성 메서드 제거 (OAuth만 사용)

	public User get(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	@Transactional
	public User updateProfile(Long userId, String nickname, String grade, String major) {
		User user = get(userId);
		user.updateProfile(nickname, grade, major);
		return user;
	}

    @Transactional
    public User completeUserProfile(Long userId, String nickname, String grade, String major, String email) {
        User user = get(userId);
        // 이메일이 변경되는 경우 중복 체크
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }
            });
            // 이메일 변경
            user.updateProfile(nickname, grade, major, email);
        } else {
            user.updateProfile(nickname, grade, major);
        }
        return user;
    }

	@Transactional
	public void delete(Long userId) {
		User user = get(userId);
		userRepository.delete(user);
	}
}
