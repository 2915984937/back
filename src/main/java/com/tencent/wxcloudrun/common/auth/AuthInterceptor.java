package com.tencent.wxcloudrun.common.auth;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一鉴权拦截器：从 Authorization: Bearer {token} 解析当前用户 ID，
 * 以 request attribute "currentUserId" 形式暴露给下游 Controller。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

  public static final String ATTR_USER_ID = "currentUserId";

  private final JwtUtil jwtUtil;

  public AuthInterceptor(@Autowired JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String auth = request.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      throw new BizException(401, "未登录");
    }
    String token = auth.substring(7).trim();
    if (token.isEmpty()) {
      throw new BizException(401, "未登录");
    }
    try {
      Long userId = jwtUtil.parseUserId(token);
      request.setAttribute(ATTR_USER_ID, userId);
      return true;
    } catch (Exception e) {
      throw new BizException(401, "登录已过期，请重新登录");
    }
  }
}

