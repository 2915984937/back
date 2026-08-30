package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.dto.LoginResult;
import com.tencent.wxcloudrun.dto.WxLoginRequest;
import com.tencent.wxcloudrun.model.User;
import com.tencent.wxcloudrun.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired MockMvc mvc;
  @MockBean UserService userService;

  private LoginResult result(String token) {
    User u = new User();
    u.setId(1L);
    u.setOpenid("o1");
    return new LoginResult(token, u, false);
  }

  @Test
  void wxLogin_成功返回token和用户() throws Exception {
    when(userService.login(any(WxLoginRequest.class), any())).thenReturn(result("tok-1"));

    mvc.perform(post("/api/user/wx-login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"abc\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.token").value("tok-1"))
        .andExpect(jsonPath("$.data.user.openid").value("o1"))
        .andExpect(jsonPath("$.data.isNewUser").value(false));
  }

  @Test
  void wxLogin_透传云托管x_wx_openid请求头() throws Exception {
    when(userService.login(any(WxLoginRequest.class), eq("hdr-openid"))).thenReturn(result("tok-h"));

    mvc.perform(post("/api/user/wx-login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"abc\"}")
            .header("x-wx-openid", "hdr-openid"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").value("tok-h"));

    verify(userService).login(any(WxLoginRequest.class), eq("hdr-openid"));
  }

  @Test
  void wxLogin_空请求体也允许() throws Exception {
    when(userService.login(any(WxLoginRequest.class), any())).thenReturn(result("tok-2"));

    mvc.perform(post("/api/user/wx-login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").value("tok-2"));
  }

  @Test
  void 微信登录失败_返回业务错误码() throws Exception {
    when(userService.login(any(WxLoginRequest.class), any()))
        .thenThrow(new com.tencent.wxcloudrun.exception.BizException(1001, "微信登录失败"));

    mvc.perform(post("/api/user/wx-login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"bad\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1001))
        .andExpect(jsonPath("$.message").value("微信登录失败"));
  }
}