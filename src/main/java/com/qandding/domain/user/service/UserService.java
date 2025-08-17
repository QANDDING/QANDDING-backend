package com.qandding.domain.user.service;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import com.qandding.global.common.error.BusinessException;
import com.qandding.global.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public Long create(String nickname, String grade, String major, String email) {
		User user = new User(nickname, grade, major, email);
		User saved = userRepository.save(user);
		return saved.getId();
	}

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
	public void delete(Long userId) {
		User user = get(userId);
		userRepository.delete(user);
	}
}
