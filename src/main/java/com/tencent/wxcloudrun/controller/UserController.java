package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.common.Result;
import com.tencent.wxcloudrun.dto.LoginResult;
import com.tencent.wxcloudrun.dto.WxLoginRequest;
import com.tencent.wxcloudrun.exception.BizException;
import com.tencent.wxcloudrun.model.User;
import com.tencent.wxcloudrun.service.UserService;
import com.tencent.wxcloudrun.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;
  private final JwtUtil jwtUtil;

  public UserController(@Autowired UserService userService,
                        @Autowired JwtUtil jwtUtil) {
    this.userService = userService;
    this.jwtUtil = jwtUtil;
  }

  /**
   * 微信登录。
   * header: x-wx-openid —— 微信云托管在来自小程序的请求注入，存在时后端直接使用，无需再调微信接口。
   * body: {code, nickname, avatar}
   */
  @PostMapping("/wx-login")
  public Result<LoginResult> wxLogin(
      @RequestBody(required = false) WxLoginRequest request,
      @RequestHeader(value = "x-wx-openid", required = false) String headerOpenid) {
    WxLoginRequest req = request != null ? request : new WxLoginRequest();
    return Result.ok(userService.login(req, headerOpenid));
  }

  /**
   * 更新当前登录用户的昵称 / 头像。
   * header: Authorization: Bearer {token}
   * body: {nickname, avatar}   只传需要更新的字段
   */
  @PostMapping("/profile")
  public Result<User> updateProfile(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestBody Map<String, String> body) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new BizException(401, "未登录");
    }
    String token = authHeader.substring(7);
    Long userId;
    try {
      userId = jwtUtil.parseUserId(token);
    } catch (Exception e) {
      throw new BizException(401, "登录已过期，请重新登录");
    }
    String nickname = body == null ? null : body.get("nickname");
    String avatar = body == null ? null : body.get("avatar");
    return Result.ok(userService.updateProfile(userId, nickname, avatar));
  }
}