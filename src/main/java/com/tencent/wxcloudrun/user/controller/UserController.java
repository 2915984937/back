package com.tencent.wxcloudrun.user.controller;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import com.tencent.wxcloudrun.common.util.Result;
import com.tencent.wxcloudrun.user.dto.LoginResult;
import com.tencent.wxcloudrun.user.dto.UpdateProfileRequest;
import com.tencent.wxcloudrun.user.dto.WxLoginRequest;
import com.tencent.wxcloudrun.user.model.User;
import com.tencent.wxcloudrun.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

  @PostMapping("/wx/login")
  public Result<LoginResult> wxLogin(@Validated @RequestBody WxLoginRequest req) {
    return Result.ok(userService.login(req));
  }

  @GetMapping("/me")
  public Result<User> me(@RequestHeader("Authorization") String authHeader) {
    Long userId = jwtUtil.parseUserId(authHeader);
    User user = userService.getCurrentUser(userId);
    if (user == null) {
      throw new BizException(BizException.CODE_USER_NOT_FOUND, "用户不存在");
    }
    return Result.ok(user);
  }

  @PutMapping("/me")
  public Result<User> updateMe(@RequestHeader("Authorization") String authHeader,
                               @RequestBody UpdateProfileRequest req) {
    Long userId = jwtUtil.parseUserId(authHeader);
    User user = userService.updateProfile(userId, req.getNickname(), req.getAvatar());
    return Result.ok(user);
  }

  @DeleteMapping("/me")
  public Result<String> deleteMe(@RequestHeader("Authorization") String authHeader) {
    Long userId = jwtUtil.parseUserId(authHeader);
    userService.anonymize(userId);
    return Result.ok("ok");
  }
}