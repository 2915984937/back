package com.tencent.wxcloudrun.model;

import java.time.LocalDateTime;

/**
 * C端用户（sys_user）。无余额/钱包：仅微信支付。
 */
public class User {

  private Long id;
  private String openid;
  private String unionid;
  private String phone;
  private String nickname;
  private String avatar;
  private Integer isStudent;
  private Integer status;
  private LocalDateTime blacklistUntil;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getOpenid() { return openid; }
  public void setOpenid(String openid) { this.openid = openid; }
  public String getUnionid() { return unionid; }
  public void setUnionid(String unionid) { this.unionid = unionid; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }
  public String getAvatar() { return avatar; }
  public void setAvatar(String avatar) { this.avatar = avatar; }
  public Integer getIsStudent() { return isStudent; }
  public void setIsStudent(Integer isStudent) { this.isStudent = isStudent; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public LocalDateTime getBlacklistUntil() { return blacklistUntil; }
  public void setBlacklistUntil(LocalDateTime blacklistUntil) { this.blacklistUntil = blacklistUntil; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}