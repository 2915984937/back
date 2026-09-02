package com.tencent.wxcloudrun.payment;

import com.tencent.wxcloudrun.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.tencent.wxcloudrun.payment.controller.WxPayCallbackController.class)
class WxPayCallbackControllerTest {

  @Autowired MockMvc mvc;
  @MockBean PaymentService paymentService;

  @Test
  void notify_回调成功返回SUCCESS() throws Exception {
    when(paymentService.handleNotify(anyString())).thenReturn("SUCCESS");

    mvc.perform(post("/api/payment/wx/notify").content("<xml/>"))
        .andExpect(status().isOk())
        .andExpect(content().string("SUCCESS"));
  }

  @Test
  void notify_回调失败返回FAIL() throws Exception {
    when(paymentService.handleNotify(anyString())).thenReturn("FAIL");

    mvc.perform(post("/api/payment/wx/notify").content("<xml/>"))
        .andExpect(status().isOk())
        .andExpect(content().string("FAIL"));
  }
}
