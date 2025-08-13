package com.qandding.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserPrincipal implements UserDetails {
	private final Long userId;
	private final String email;
	private final String nickname;
	private final List<SimpleGrantedAuthority> authorities;

	public CustomUserPrincipal(Long userId, String email, String nickname, List<SimpleGrantedAuthority> authorities) {
		this.userId = userId;
		this.email = email;
		this.nickname = nickname;
		this.authorities = authorities;
	}

	public Long getUserId() { return userId; }
	public String getEmail() { return email; }
	public String getNickname() { return nickname; }

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
	@Override
	public String getPassword() { return ""; }
	@Override
	public String getUsername() { return email; }
	@Override
	public boolean isAccountNonExpired() { return true; }
	@Override
	public boolean isAccountNonLocked() { return true; }
	@Override
	public boolean isCredentialsNonExpired() { return true; }
	@Override
	public boolean isEnabled() { return true; }
}
