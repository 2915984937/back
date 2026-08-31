package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.LoginResult;
import com.tencent.wxcloudrun.dto.WxLoginRequest;
import com.tencent.wxcloudrun.model.User;

public interface UserService {

  /**
   * 微信登录：优先使用微信云托管注入的请求头 x-wx-openid；否则用 code 调 jscode2session 换取 openid。
   * 已存在用户复用并返回 token；新用户自动注册。
   */
  LoginResult login(WxLoginRequest request, String headerOpenid);

  /**
   * 更新当前登录用户的昵称 / 头像。
   * @param userId 从 JWT 解析出的用户 ID
   * @param nickname 新昵称（null/空则不改）
   * @param avatar 新头像（null/空则不改）
   * @return 更新后的用户
   */
  User updateProfile(Long userId, String nickname, String avatar);
}