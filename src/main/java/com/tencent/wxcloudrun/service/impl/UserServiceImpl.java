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
      user.setNickname(request.getNickname());
      user.setAvatar(request.getAvatar());
      userMapper.insert(user); // useGeneratedKeys 回填 id
      isNewUser = true;
    } else if (StringUtils.hasText(request.getNickname()) || StringUtils.hasText(request.getAvatar())) {
      if (StringUtils.hasText(request.getNickname())) user.setNickname(request.getNickname());
      if (StringUtils.hasText(request.getAvatar())) user.setAvatar(request.getAvatar());
      userMapper.updateProfile(user);
    }

    // 3. 签发 token
    String token = jwtUtil.generate(user.getId(), openid);
    return new LoginResult(token, user, isNewUser);
  }
}