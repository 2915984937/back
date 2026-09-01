package com.tencent.wxcloudrun.user.dto;

import javax.validation.constraints.Size;

/**
 * 小程序 wx-login 请求体。
 * code 是前端 wx.login() 返回的临时凭证，必填。
 */
public class WxLoginRequest {

  /** wx.login() 返回的临时登录凭证 code，必填。 */
  private String code;

  /** 可选：前端展示昵称，不超过 20 字符。 */
  @Size(max = 20, message = "昵称不能超过 20 个字符")
  private String nickname;

  /** 可选：头像 URL（云存储 fileID 或临时路径）。 */
  private String avatar;

  /** 可选：getPhoneNumber 返回的 code，后端用它换手机号。 */
  private String phoneCode;

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getAvatar() { return avatar; }
  public void setAvatar(String avatar) { this.avatar = avatar; }
  public String getPhoneCode() { return phoneCode; }
  public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
}
