package com.tencent.wxcloudrun.common;

import com.tencent.wxcloudrun.common.auth.AuthInterceptor;
import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

  @Mock JwtUtil jwtUtil;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock Object handler;
  @InjectMocks
  AuthInterceptor interceptor;

  @Test
  void 无Authorization头_抛401() {
    when(request.getHeader("Authorization")).thenReturn(null);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(BizException.class)
        .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(401));
  }

  @Test
  void 不是Bearer前缀_抛401() {
    when(request.getHeader("Authorization")).thenReturn("token-without-bearer");

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(BizException.class);
  }

  @Test
  void Bearer后没token_抛401() {
    when(request.getHeader("Authorization")).thenReturn("Bearer ");

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(BizException.class);
    verify(jwtUtil, never()).parseUserId("");
  }

  @Test
  void token过期_抛401带过期提示() {
    when(request.getHeader("Authorization")).thenReturn("Bearer abc.expired.xyz");
    when(jwtUtil.parseUserId("abc.expired.xyz"))
        .thenThrow(new JwtException("expired"));

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(BizException.class)
        .satisfies(e -> {
          BizException be = (BizException) e;
          assertThat(be.getCode()).isEqualTo(401);
          assertThat(be.getMessage()).contains("过期");
        });
  }

  @Test
  void token合法_注入currentUserId到requestAttribute() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
    when(jwtUtil.parseUserId("valid.token.here")).thenReturn(42L);

    boolean result = interceptor.preHandle(request, response, handler);

    assertThat(result).isTrue();
    verify(request).setAttribute(AuthInterceptor.ATTR_USER_ID, 42L);
  }
}
