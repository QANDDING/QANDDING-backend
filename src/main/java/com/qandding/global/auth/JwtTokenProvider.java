package com.qandding.global.auth;

import com.qandding.domain.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secretKey,
      @Value("${app.jwt.access-expiration}") long accessTokenExpiration,
      @Value("${app.jwt.refresh-expiration}") long refreshTokenExpiration) {
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  /**
   * 사용자의 Access Token을 생성합니다.
   * @param user 사용자 엔티티
   * @return 생성된 Access Token 문자열
   */
  public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("email", user.getEmail());

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(user.getEmail())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * 사용자의 Refresh Token을 생성합니다.
   * @param user 사용자 엔티티
   * @return 생성된 Refresh Token 문자열
   */
  public String generateRefreshToken(User user) {
    return Jwts.builder()
        .setSubject(user.getEmail())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * JWT 토큰을 검증합니다.
   * @param token 검증할 토큰
   * @return 유효하면 true, 아니면 false
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.warn("잘못된 JWT 서명입니다.", e);
    } catch (ExpiredJwtException e) {
      log.warn("만료된 JWT 토큰입니다.", e);
    } catch (UnsupportedJwtException e) {
      log.warn("지원되지 않는 JWT 토큰입니다.", e);
    } catch (IllegalArgumentException e) {
      log.warn("JWT 토큰이 잘못되었습니다.", e);
    }
    return false;
  }

  /**
   * 토큰에서 Claims 정보를 추출합니다.
   * @param token 정보를 추출할 토큰
   * @return Claims 객체
   */
  private Claims getClaimsFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * 토큰에서 사용자 ID를 추출합니다.
   * @param token 사용자 ID를 추출할 토큰
   * @return 사용자 ID (Long)
   */
  public Long getUserIdFromToken(String token) {
    return getClaimsFromToken(token).get("userId", Long.class);
  }
}