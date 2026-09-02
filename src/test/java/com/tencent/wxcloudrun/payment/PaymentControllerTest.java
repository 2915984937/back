package com.tencent.wxcloudrun.payment;

import com.tencent.wxcloudrun.common.util.JwtUtil;
import com.tencent.wxcloudrun.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.tencent.wxcloudrun.payment.controller.PaymentController.class)
class PaymentControllerTest {

  private static final Long USER_ID = 42L;
  private static final String TOKEN = "Bearer valid.token";

  @Autowired MockMvc mvc;
  @MockBean PaymentService paymentService;
  @MockBean JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    when(jwtUtil.parseUserId(anyString())).thenReturn(USER_ID);
  }

  private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req) {
    return req.header("Authorization", TOKEN);
  }

  @Test
  void prepay_返回支付参数() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("package", "prepay_id=xyz");
    when(paymentService.createPrepay(any(), eq(1L))).thenReturn(params);

    mvc.perform(withAuth(post("/api/payment/prepay")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderId\":1}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.package").value("prepay_id=xyz"));
  }
}
