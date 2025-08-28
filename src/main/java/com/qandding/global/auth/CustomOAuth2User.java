package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

/**
 * OidcUser를 확장하여 우리 서비스의 User 엔티티를 직접 포함하도록 만든 클래스.
 * 이를 통해 DB 재조회 없이 SuccessHandler로 사용자 정보를 전달할 수 있습니다.
 */
@Getter
public class CustomOAuth2User extends DefaultOidcUser {

  private final User user;

  public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
      OidcIdToken idToken, OidcUserInfo userInfo, User user) {
    super(authorities, idToken, userInfo);
    this.user = user;
  }
}