package com.tencent.wxcloudrun.common;

import com.tencent.wxcloudrun.common.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

  /** 构造但不走 Spring，手动注入配置后 init。 */
  private JwtUtil newUtil(long expireHours) {
    JwtUtil util = new JwtUtil();
    ReflectionTestUtils.setField(util, "secret", "study-room-test-secret-0123456789abcdefg");
    ReflectionTestUtils.setField(util, "expireHours", expireHours);
    util.init();
    return util;
  }

  @Test
  void 签发可解析_往返一致() {
    JwtUtil util = newUtil(1);
    String token = util.generate(123L, "openid-x");
    assertThat(token).isNotBlank();
    assertThat(util.parseUserId(token)).isEqualTo(123L);
  }

  @Test
  void 过期token_解析抛出异常() {
    JwtUtil util = newUtil(0); // 0 小时 → 立即过期
    String token = util.generate(1L, "o1");
    assertThatThrownBy(() -> util.parseUserId(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void 篡改token_解析抛出异常() {
    JwtUtil util = newUtil(1);
    String token = util.generate(1L, "o1");
    String tampered = token.substring(0, token.length() - 2) + "zz";
    assertThatThrownBy(() -> util.parseUserId(tampered))
        .isInstanceOf(JwtException.class);
  }
}