package com.tencent.wxcloudrun.common;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import com.tencent.wxcloudrun.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 GlobalExceptionHandler 能正确拦截 BizException / RuntimeException / 参数校验异常。
 * 通过真实 UserController 的接口路径触发异常（比自造内部 Controller 更贴近生产）。
 */
@WebMvcTest(com.tencent.wxcloudrun.user.controller.UserController.class)
class GlobalExceptionHandlerTest {

  @Autowired org.springframework.test.web.servlet.MockMvc mvc;
  @Autowired ObjectMapper mapper;

  @MockBean UserService userService;
  @MockBean JwtUtil jwtUtil;  // AuthInterceptor 启动需要

  @Test
  void bizException_转为Result() throws Exception {
    when(userService.login(any()))
        .thenThrow(new BizException(1001, "微信登录失败"));

    mvc.perform(post("/api/user/wx/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"bad\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1001))
        .andExpect(jsonPath("$.message").value("微信登录失败"));
  }

  @Test
  void 未知异常_兜底500() throws Exception {
    when(userService.login(any()))
        .thenThrow(new RuntimeException("未知数据库错误"));

    mvc.perform(post("/api/user/wx/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"xyz\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(500));
  }

  @Test
  void 参数校验失败_返回400带字段错误信息() throws Exception {
    String longNick = "这是一个超过二十个字的昵称超过二十个字了哦";  // 22字 > @Size(max=20)
    mvc.perform(post("/api/user/wx/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nickname\":\"" + longNick + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("昵称不能超过 20 个字符"));
  }
}
