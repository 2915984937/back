package com.tencent.wxcloudrun.user;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import com.tencent.wxcloudrun.user.dto.LoginResult;
import com.tencent.wxcloudrun.user.dto.WxLoginRequest;
import com.tencent.wxcloudrun.user.model.User;
import com.tencent.wxcloudrun.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 只负责路由/参数透传/异常映射。
 * AuthInterceptor 由 @WebMvcTest 自动加载（WebMvcConfig），
 * 所以测试带 Authorization header 让拦截器走通，由 mock JwtUtil 提供 userId。
 */
@WebMvcTest(com.tencent.wxcloudrun.user.controller.UserController.class)
class UserControllerTest {

  private static final Long USER_ID = 42L;
  private static final String TOKEN = "Bearer valid.token.here";

  @Autowired MockMvc mvc;
  @MockBean UserService userService;
  @MockBean JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    when(jwtUtil.parseUserId(anyString())).thenReturn(USER_ID);
  }

  private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req) {
    return req.header("Authorization", TOKEN);
  }

  // ---- POST /api/user/wx/login ----

  @Test
  void wxLogin_传code走service() throws Exception {
    LoginResult result = new LoginResult("t-2", new User(), true);
    when(userService.login(any(WxLoginRequest.class))).thenReturn(result);

    mvc.perform(post("/api/user/wx/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"c001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.isNewUser").value(true));
  }

  @Test
  void wxLogin_service抛BizException() throws Exception {
    when(userService.login(any(WxLoginRequest.class)))
        .thenThrow(new BizException(1006, "账号已被封禁"));

    mvc.perform(post("/api/user/wx/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"bad\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1006));
  }

  // ---- GET /api/user/me ----

  @Test
  void me_不带token_401() throws Exception {
    mvc.perform(get("/api/user/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void me_正常返回用户() throws Exception {
    User u = new User();
    u.setId(42L);
    u.setNickname("张三");
    when(userService.getCurrentUser(USER_ID)).thenReturn(u);

    mvc.perform(withAuth(get("/api/user/me")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(42))
        .andExpect(jsonPath("$.data.nickname").value("张三"));
  }

  // ---- PUT /api/user/me ----

  @Test
  void updateMe_不带token_401() throws Exception {
    mvc.perform(put("/api/user/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"x\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void updateMe_正常更新() throws Exception {
    User updated = new User();
    updated.setId(42L);
    updated.setNickname("新昵称");
    when(userService.updateProfile(USER_ID, "新昵称", null)).thenReturn(updated);

    mvc.perform(withAuth(put("/api/user/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"新昵称\"}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(42));
  }

  @Test
  void updateMe_用户不存在_返回BizException() throws Exception {
    when(userService.updateProfile(USER_ID, "x", null))
        .thenThrow(new BizException(1004, "用户不存在"));

    mvc.perform(withAuth(put("/api/user/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"x\"}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1004));
  }

  // ---- DELETE /api/user/me ----

  @Test
  void deleteMe_不带token_401() throws Exception {
    mvc.perform(delete("/api/user/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(401));
  }

  @Test
  void deleteMe_正常匿名化注销() throws Exception {
    mvc.perform(withAuth(delete("/api/user/me")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  void deleteMe_用户不存在_返回错误码() throws Exception {
    doThrow(new BizException(1004, "用户不存在"))
        .when(userService).anonymize(USER_ID);

    mvc.perform(withAuth(delete("/api/user/me")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1004));
  }
}