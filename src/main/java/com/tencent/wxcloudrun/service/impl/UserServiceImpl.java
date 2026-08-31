package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.api.WxApiClient;
import com.tencent.wxcloudrun.dao.UserMapper;
import com.tencent.wxcloudrun.dto.LoginResult;
import com.tencent.wxcloudrun.dto.WxLoginRequest;
import com.tencent.wxcloudrun.exception.BizException;
import com.tencent.wxcloudrun.model.User;
import com.tencent.wxcloudrun.service.UserService;
import com.tencent.wxcloudrun.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final WxApiClient wxApiClient;
  private final JwtUtil jwtUtil;

  public UserServiceImpl(@Autowired UserMapper userMapper,
                         @Autowired WxApiClient wxApiClient,
                         @Autowired JwtUtil jwtUtil) {
    this.userMapper = userMapper;
    this.wxApiClient = wxApiClient;
    this.jwtUtil = jwtUtil;
  }

  @Override
  public LoginResult login(WxLoginRequest request, String headerOpenid) {
    // 1. 解析 openid：优先信任云托管注入的 x-wx-openid，否则调用微信换取
    String openid = StringUtils.hasText(headerOpenid) ? headerOpenid.trim() : wxApiClient.code2Session(request.getCode());
    if (!StringUtils.hasText(openid)) {
      throw new BizException(1001, "登录失败：未获取到 openid");
    }

    // 2. 查找或创建用户
    User user = userMapper.selectByOpenid(openid);
    boolean isNewUser = false;
    if (user == null) {
      user = new User();
      user.setOpenid(openid);
      // 昵称：传了就用，否则给默认值（openid 后 8 位便于区分）
      if (StringUtils.hasText(request.getNickname())) {
        user.setNickname(request.getNickname());
      } else {
        user.setNickname("微信用户_" + openid.substring(Math.max(0, openid.length() - 8)));
      }
      user.setAvatar(request.getAvatar());
      // phoneCode 换手机号
      if (StringUtils.hasText(request.getPhoneCode())) {
        user.setPhone(wxApiClient.getPhoneNumber(request.getPhoneCode()));
      }
      userMapper.insert(user); // useGeneratedKeys 回填 id
      isNewUser = true;
    } else {
      boolean needUpdate = false;
      if (StringUtils.hasText(request.getNickname())) {
        user.setNickname(request.getNickname());
        needUpdate = true;
      }
      if (StringUtils.hasText(request.getAvatar())) {
        user.setAvatar(request.getAvatar());
        needUpdate = true;
      }
      // 手机号：如果传了 phoneCode 且用户还没绑定手机号，则绑定
      if (StringUtils.hasText(request.getPhoneCode())
          && !StringUtils.hasText(user.getPhone())) {
        user.setPhone(wxApiClient.getPhoneNumber(request.getPhoneCode()));
        needUpdate = true;
      }
      if (needUpdate) {
        userMapper.updateProfile(user);
      }
    }

    // 3. 签发 token
    String token = jwtUtil.generate(user.getId(), openid);
    return new LoginResult(token, user, isNewUser);
  }

  @Override
  public User updateProfile(Long userId, String nickname, String avatar) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BizException(1004, "用户不存在");
    }
    boolean needUpdate = false;
    if (StringUtils.hasText(nickname)) {
      user.setNickname(nickname.trim());
      needUpdate = true;
    }
    if (StringUtils.hasText(avatar)) {
      user.setAvatar(avatar.trim());
      needUpdate = true;
    }
    if (needUpdate) {
      userMapper.updateProfile(user);
    }
    return user;
  }
}
