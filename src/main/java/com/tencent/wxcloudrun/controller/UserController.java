package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.common.Result;
import com.tencent.wxcloudrun.dto.LoginResult;
import com.tencent.wxcloudrun.dto.WxLoginRequest;
import com.tencent.wxcloudrun.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;

  public UserController(@Autowired UserService userService) {
    this.userService = userService;
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
}