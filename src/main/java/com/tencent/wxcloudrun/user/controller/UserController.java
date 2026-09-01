package com.tencent.wxcloudrun.user.controller;

import com.tencent.wxcloudrun.common.auth.AuthInterceptor;
import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.Result;
import com.tencent.wxcloudrun.user.dto.LoginResult;
import com.tencent.wxcloudrun.user.dto.UpdateProfileRequest;
import com.tencent.wxcloudrun.user.dto.WxLoginRequest;
import com.tencent.wxcloudrun.user.model.User;
import com.tencent.wxcloudrun.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;

  public UserController(@Autowired UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/wx/login")
  public Result<LoginResult> wxLogin(@Validated @RequestBody WxLoginRequest req) {
    return Result.ok(userService.login(req));
  }

  @GetMapping("/me")
  public Result<User> me(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    User user = userService.getCurrentUser(userId);
    if (user == null) {
      throw new BizException(BizException.CODE_USER_NOT_FOUND, "用户不存在");
    }
    return Result.ok(user);
  }

  @PutMapping("/me")
  public Result<User> updateMe(HttpServletRequest request,
                               @Validated @RequestBody UpdateProfileRequest req) {
    Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    User user = userService.updateProfile(userId, req.getNickname(), req.getAvatar());
    return Result.ok(user);
  }

  @DeleteMapping("/me")
  public Result<String> deleteMe(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    userService.anonymize(userId);
    return Result.ok("ok");
  }
}