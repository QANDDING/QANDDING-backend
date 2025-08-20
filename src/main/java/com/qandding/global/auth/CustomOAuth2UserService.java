package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Value("${app.oauth.allowed-domain}")
    private String allowedDomain;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        
        // OAuth2 사용자 정보를 Map으로 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        validateDomain(email);
        User user = saveOrUpdate(email, name);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName);
    }

    private void validateDomain(String email) {
        if (email == null || !email.endsWith("@" + allowedDomain)) {
            throw new OAuth2AuthenticationException("허용되지 않은 이메일 도메인입니다.");
        }
    }
    
    private User saveOrUpdate(String email, String name) {
        User user = userRepository.findByEmail(email)
                .map(entity -> {
                    if (!entity.isEmailVerified()) {
                        entity.markEmailVerified();
                    }
                    return entity;
                })
                .orElseGet(() -> {
                    String displayName = name;
                    if (displayName != null && displayName.contains("/")) {
                        displayName = displayName.split("/")[0];
                    }
                    if (displayName == null || displayName.isBlank()) {
                        displayName = "사용자";
                    }
                    return new User(displayName, "", "", email);
                });

        return userRepository.save(user);
    }
}
