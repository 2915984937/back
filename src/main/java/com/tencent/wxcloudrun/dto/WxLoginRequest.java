package com.tencent.wxcloudrun.dto;

/**
 * 小程序 wx-login 请求体。
 */
public class WxLoginRequest {

  /** wx.login() 返回的临时登录凭证 code（云托管带 x-wx-openid 时可省略）。 */
  private String code;
  /** 可选：前端展示昵称 */
  private String nickname;
  /** 可选：头像 URL */
  private String avatar;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getAvatar() { return avatar; }
  public void setAvatar(String avatar) { this.avatar = avatar; }
}