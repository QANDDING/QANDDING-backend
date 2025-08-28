package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import com.qandding.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    // 부모 클래스(OidcUserService)의 loadUser를 호출하여 OidcUser 객체를 받습니다.
    OidcUser oidcUser = super.loadUser(userRequest);

    String email = oidcUser.getEmail();
    String name = oidcUser.getFullName();

    // DB에서 사용자를 조회하거나 새로 생성합니다.
    User user = saveOrUpdate(email, name);

    // OIDC 정보와 우리 User 엔티티를 담은 CustomOAuth2User를 생성하여 반환합니다.
    return new CustomOAuth2User(
        oidcUser.getAuthorities(),
        oidcUser.getIdToken(),
        oidcUser.getUserInfo(),
        user
    );
  }

  private User saveOrUpdate(String email, String name) {
    User user = userRepository.findByEmail(email)
        .map(entity -> {
          log.info("기존 사용자 발견: {}", email);
          // 기존 사용자의 경우 추가적인 업데이트 로직이 필요하다면 여기에 작성
          return entity;
        })
        .orElseGet(() -> {
          log.info("새 사용자 생성: {}", email);
          String displayName = name;
          if (name != null && name.contains("/")) {
            displayName = name.split("/")[0];
          }
          return new User(displayName, null, null, email);
        });

    // saveAndFlush를 사용하여 트랜잭션 커밋 시점과 관계없이 즉시 DB에 반영합니다.
    return userRepository.saveAndFlush(user);
  }
}