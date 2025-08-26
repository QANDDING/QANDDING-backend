package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Value("${app.oauth.allowed-domain}")
    private String allowedDomain;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            log.info("=== CustomOAuth2UserService.loadUser 시작 ===");
            log.info("허용된 도메인: {}", allowedDomain);
            log.info("Client Registration ID: {}", userRequest.getClientRegistration().getRegistrationId());
            
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
            log.info("DefaultOAuth2UserService 위임 객체 생성");
            
            OAuth2User oAuth2User = delegate.loadUser(userRequest);
            log.info("위임 객체를 통한 OAuth2User 로드 성공");
            
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();
            
            log.info("Registration ID: {}", registrationId);
            log.info("User Name Attribute Name: {}", userNameAttributeName);
            
            // OAuth2 사용자 정보를 Map으로 추출
            Map<String, Object> attributes = oAuth2User.getAttributes();
            log.info("OAuth2 Attributes 전체: {}", attributes);
            
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            
            log.info("추출된 이메일: {}", email);
            log.info("추출된 이름: {}", name);
            
            // 도메인 검증
            log.info("이메일 도메인 검증 시작");
            validateDomain(email);
            log.info("이메일 도메인 검증 통과");
            
            // 사용자 저장/업데이트
            log.info("사용자 저장/업데이트 시작");
            User user = saveOrUpdate(email, name);
            log.info("사용자 저장/업데이트 완료: ID={}, 이메일={}", user.getId(), user.getEmail());
            
            // DefaultOAuth2User 생성
            log.info("DefaultOAuth2User 객체 생성 시작");
            DefaultOAuth2User result = new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    userNameAttributeName);
            log.info("DefaultOAuth2User 객체 생성 완료");
            
            log.info("=== CustomOAuth2UserService.loadUser 완료 ===");
            return result;
            
        } catch (Exception e) {
            log.error("=== CustomOAuth2UserService.loadUser에서 오류 발생 ===", e);
            log.error("오류 타입: {}", e.getClass().getSimpleName());
            log.error("오류 메시지: {}", e.getMessage());
            throw e;
        }
    }

    private void validateDomain(String email) {
        log.info("도메인 검증 시작: {}", email);
        if (email == null || !email.endsWith("@" + allowedDomain)) {
            log.error("허용되지 않은 이메일 도메인: {} (허용된 도메인: {})", email, allowedDomain);
            throw new OAuth2AuthenticationException("허용되지 않은 이메일 도메인입니다.");
        }
        log.info("도메인 검증 통과: {}", email);
    }
    
    private User saveOrUpdate(String email, String name) {
        log.info("사용자 저장/업데이트 시작: email={}, name={}", email, name);
        
        User user = userRepository.findByEmail(email)
                .map(entity -> {
                    log.info("기존 사용자 발견: ID={}, 닉네임={}", entity.getId(), entity.getNickname());
                    if (!entity.isEmailVerified()) {
                        log.info("이메일 인증 상태 업데이트: false -> true");
                        entity.markEmailVerified();
                    } else {
                        log.info("이미 이메일 인증됨");
                    }
                    return entity;
                })
                .orElseGet(() -> {
                    log.info("새 사용자 생성 시작");
                    String displayName = name;
                    if (displayName != null && displayName.contains("/")) {
                        displayName = displayName.split("/")[0];
                        log.info("이름에서 '/' 제거: {} -> {}", name, displayName);
                    }
                    if (displayName == null || displayName.isBlank()) {
                        displayName = "사용자";
                        log.info("기본 이름 사용: {}", displayName);
                    }
                    User newUser = new User(displayName, "", "", email);
                    log.info("새 사용자 객체 생성: {}", newUser);
                    return newUser;
                });

        User savedUser = userRepository.save(user);
        log.info("사용자 저장 완료: ID={}, 이메일={}", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }
}
