package com.tencent.wxcloudrun.user.dto;

/**
 * PUT /api/user/me 请求体。
 * nickname / avatar 可选：只传要更新的字段。
 */
public class UpdateProfileRequest {

  /** 昵称，不超过 20 字符。 */
  private String nickname;

  /** 头像 fileID。 */
  private String avatar;

  /** 手机号（明文）。仅当传入合法 11 位号码时后端才更新。 */
  private String phone;

  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getAvatar() { return avatar; }
  public void setAvatar(String avatar) { this.avatar = avatar; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
}