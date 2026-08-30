package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.LoginResult;
import com.tencent.wxcloudrun.dto.WxLoginRequest;

public interface UserService {

  /**
   * 微信登录：优先使用微信云托管注入的请求头 x-wx-openid；否则用 code 调 jscode2session 换取 openid。
   * 已存在用户复用并返回 token；新用户自动注册。
   */
  LoginResult login(WxLoginRequest request, String headerOpenid);
}