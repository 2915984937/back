package com.tencent.wxcloudrun.user.service;

import com.tencent.wxcloudrun.wx.WxApiClient;
import com.tencent.wxcloudrun.user.dao.UserMapper;
import com.tencent.wxcloudrun.user.dto.LoginResult;
import com.tencent.wxcloudrun.user.dto.WxLoginRequest;
import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.user.model.User;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

  private final UserMapper userMapper;
  private final WxApiClient wxApiClient;
  private final JwtUtil jwtUtil;

  public UserService(@Autowired UserMapper userMapper,
                     @Autowired WxApiClient wxApiClient,
                     @Autowired JwtUtil jwtUtil) {
    this.userMapper = userMapper;
    this.wxApiClient = wxApiClient;
    this.jwtUtil = jwtUtil;
  }

  /**
   * 微信登录：code → jscode2session 换 openid。
   * 已注销账号（openid 已清空为 NULL）不会被 selectByOpenid 查到 → 走新用户注册分支 → 可重新注册。
   * 封禁判定（全局统一）：
   *   status=1 永久封禁 → 拒绝
   *   ban_until 未过 → 临时封禁 → 拒绝
   *   ban_until 已过  → 懒加载解禁后放行
   */
  @Transactional
  public LoginResult login(WxLoginRequest request) {
    String openid = wxApiClient.code2Session(request.getCode());
    if (!StringUtils.hasText(openid)) {
      throw new BizException(BizException.CODE_WX_LOGIN_FAILED, "登录失败：未获取到 openid");
    }

    User user = userMapper.selectByOpenid(openid);  // 自动过滤 deleted=1
    if (user == null) {
      // 新用户（或已注销账号 openid 已清空 → 视同新用户，可重新注册）
      user = new User();
      user.setOpenid(openid);
      if (StringUtils.hasText(request.getNickname())) {
        user.setNickname(request.getNickname());
      } else {
        user.setNickname("微信用户_" + openid.substring(Math.max(0, openid.length() - 8)));
      }
      user.setAvatar(request.getAvatar());
      if (StringUtils.hasText(request.getPhoneCode())) {
        user.setPhone(wxApiClient.getPhoneNumber(request.getPhoneCode()));
      }
      userMapper.insert(user);
      userMapper.updateLastLogin(user.getId());
      return new LoginResult(jwtUtil.generate(user.getId(), openid), user, true);
    }

    // 老用户：封禁判定
    // 1) 永久封禁 status=1 → 直接拒绝
    if (user.getStatus() != null && user.getStatus() == 1) {
      throw new BizException(BizException.CODE_USER_BANNED, "账号已被永久封禁");
    }
    // 2) 懒加载解禁临时封禁
    userMapper.unbanIfExpired(user.getId());
    // 3) 再次确认：解禁后或临时封禁未到期
    //    unbanIfExpired 只清 ban_until 不动 status=0，所以 status 已经是 0 了
    //    但如果 ban_until 还没到 → unbanIfExpired 不动 → 我们再查一次拿最新 ban_until
    if (user.getBanUntil() != null && user.getBanUntil().isAfter(java.time.LocalDateTime.now())) {
      throw new BizException(BizException.CODE_USER_BANNED, "账号已被封禁至 " + user.getBanUntil());
    }

    boolean needUpdate = false;
    if (StringUtils.hasText(request.getNickname())) {
      user.setNickname(request.getNickname());
      needUpdate = true;
    }
    if (StringUtils.hasText(request.getAvatar())) {
      user.setAvatar(request.getAvatar());
      needUpdate = true;
    }
    if (StringUtils.hasText(request.getPhoneCode())
        && !StringUtils.hasText(user.getPhone())) {
      user.setPhone(wxApiClient.getPhoneNumber(request.getPhoneCode()));
      needUpdate = true;
    }
    if (needUpdate) {
      userMapper.updateProfile(user);
    }
    userMapper.updateLastLogin(user.getId());

    String token = jwtUtil.generate(user.getId(), openid);
    return new LoginResult(token, user, false);
  }

  /** 获取当前登录用户，自动懒加载解禁 */
  public User getCurrentUser(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      return null;
    }
    // 懒加载：临时封禁到期自动清 ban_until
    if (user.getBanUntil() != null && user.getBanUntil().isBefore(java.time.LocalDateTime.now())) {
      userMapper.unbanIfExpired(userId);
      user.setBanUntil(null);
    }
    return user;
  }

  /** 更新昵称 / 头像 / 手机号 */
  @Transactional
  public User updateProfile(Long userId, String nickname, String avatar, String phone) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BizException(BizException.CODE_USER_NOT_FOUND, "用户不存在");
    }
    if (user.getStatus() != null && user.getStatus() == 1) {
      throw new BizException(BizException.CODE_USER_BANNED, "账号已被永久封禁");
    }
    if (user.getBanUntil() != null && user.getBanUntil().isAfter(java.time.LocalDateTime.now())) {
      throw new BizException(BizException.CODE_USER_BANNED, "账号已被临时封禁");
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
    if (StringUtils.hasText(phone)) {
      if (!phone.matches("^1[3-9]\\d{9}$")) {
        throw new BizException(400, "手机号格式不正确");
      }
      user.setPhone(phone.trim());
      needUpdate = true;
    }
    if (needUpdate) {
      userMapper.updateProfile(user);
    }
    return user;
  }

  /**
   * 注销账号（匿名化）：清空 openid / phone / nickname / avatar，
   * 保留 id 供历史订单关联。注销后该微信可重新注册新账号。
   */
  @Transactional
  public void anonymize(Long userId) {
    int rows = userMapper.anonymize(userId);
    if (rows == 0) {
      throw new BizException(BizException.CODE_USER_NOT_FOUND, "用户不存在或已注销");
    }
  }
}