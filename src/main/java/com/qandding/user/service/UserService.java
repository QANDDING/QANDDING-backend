package com.qandding.user.service;

import com.qandding.common.error.BusinessException;
import com.qandding.common.error.ErrorCode;
import com.qandding.user.domain.User;
import com.qandding.user.repository.UserRepository;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

	private static final String ALLOWED_DOMAIN = "@mju.ac.kr";

	@Transactional
	public Long create(String nickname, String grade, String major, @Email String email) {
		validateSchoolEmail(email);
		userRepository.findByEmail(email).ifPresent(u -> { throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS); });
		User user = new User(nickname, grade, major, email);
		return userRepository.save(user).getId();
	}

	@Transactional(readOnly = true)
	public User get(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public User getByEmail(String email) {
		return userRepository.findByEmail(email).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public List<User> list() {
		return userRepository.findAll();
	}

	@Transactional
	public void delete(Long id) {
		userRepository.deleteById(id);
	}

	@Transactional
	public User updateProfile(Long userId, String nickname, String grade, String major) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		user.updateProfile(nickname, grade, major);
		return user;
	}

	private void validateSchoolEmail(String email) {
		if (email == null || !email.endsWith(ALLOWED_DOMAIN)) {
			throw new BusinessException(ErrorCode.INVALID_SCHOOL_EMAIL);
		}
	}
}
