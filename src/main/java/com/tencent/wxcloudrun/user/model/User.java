package com.tencent.wxcloudrun.user.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * C 端用户（sys_user）。
 *
 * 封禁口径（全局统一）：
 *   可用 = status = 0 AND (ban_until IS NULL OR ban_until < NOW())
 *   status = 1 表示人工永久封禁，不看 ban_until
 *   ban_until 非空仅代表临时封禁到期前自动解禁（懒加载）
 *
 * 注销口径：
 *   匿名化（不是物理删除）—— 清空 openid / nickname / avatar / phone，
 *   保留 id 以保证历史订单等关联数据不悬空。
 */
public class User {

  private Long id;
  private String openid;       // 注销后清空，可重新注册
  private String phone;        // 明文
  private String nickname;
  private String avatar;
  private Integer gender;      // 0-未知 1-男 2-女
  private BigDecimal balance;
  private Integer status;      // 0-正常 1-永久封禁（人工）
  private LocalDateTime banUntil;
  private Integer deleted;     // 0-正常 1-已注销（匿名化）
  private LocalDateTime lastLoginTime;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getOpenid() { return openid; }
  public void setOpenid(String openid) { this.openid = openid; }

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }

  public String getNickname() { return nickname; }
  public void setNickname(String nickname) { this.nickname = nickname; }

  public String getAvatar() { return avatar; }
  public void setAvatar(String avatar) { this.avatar = avatar; }

  public Integer getGender() { return gender; }
  public void setGender(Integer gender) { this.gender = gender; }

  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }

  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }

  public LocalDateTime getBanUntil() { return banUntil; }
  public void setBanUntil(LocalDateTime banUntil) { this.banUntil = banUntil; }

  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }

  public LocalDateTime getLastLoginTime() { return lastLoginTime; }
  public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }

  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

  /** 按全局统一口径判定是否可用 */
  public boolean isAvailable() {
    if (deleted != null && deleted == 1) return false;
    if (status != null && status == 1) return false;  // 永久封禁
    if (banUntil != null && banUntil.isAfter(LocalDateTime.now())) return false;
    return true;
  }
}