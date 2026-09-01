package com.tencent.wxcloudrun.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.wxcloudrun.user.model.User;

/**
 * wx-login 响应体：token + 用户信息。
 */
public class LoginResult {

  private String token;
  private User user;
  private boolean isNewUser;

  public LoginResult(String token, User user, boolean isNewUser) {
    this.token = token;
    this.user = user;
    this.isNewUser = isNewUser;
  }

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  @JsonProperty("isNewUser") public boolean isNewUser() { return isNewUser; }
  public void setNewUser(boolean newUser) { isNewUser = newUser; }
}