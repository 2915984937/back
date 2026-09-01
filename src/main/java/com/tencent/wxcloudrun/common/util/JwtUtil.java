package com.tencent.wxcloudrun.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 签发与解析（HS256）。密钥长度需 ≥ 32 字节。
 */
@Component
public class JwtUtil {

  @Value("${app.jwt.secret:study-room-dev-secret-change-me-please}")
  private String secret;

  @Value("${app.jwt.expire-hours:168}")
  private long expireHours;

  private Key key;

  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /** 签发 token：subject=userId，附带 openid。 */
  public String generate(Long userId, String openid) {
    Date now = new Date();
    Date expiresAt = new Date(now.getTime() + expireHours * 3600_000L);
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .claim("openid", openid)
        .setIssuedAt(now)
        .setExpiration(expiresAt)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /** 解析并返回 subject（userId）；token 非法或过期抛 JwtException。 */
  public Long parseUserId(String token) {
    // 防御性：兼容 "Bearer xxx" 格式
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7).trim();
    }
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
    return Long.valueOf(claims.getSubject());
  }
}