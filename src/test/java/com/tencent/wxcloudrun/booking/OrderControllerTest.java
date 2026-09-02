package com.tencent.wxcloudrun.booking;

import com.tencent.wxcloudrun.booking.dto.OrderVO;
import com.tencent.wxcloudrun.booking.service.OrderService;
import com.tencent.wxcloudrun.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.tencent.wxcloudrun.booking.controller.OrderController.class)
class OrderControllerTest {

  private static final Long USER_ID = 42L;
  private static final String TOKEN = "Bearer valid.token";

  @Autowired MockMvc mvc;
  @MockBean OrderService orderService;
  @MockBean JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    when(jwtUtil.parseUserId(anyString())).thenReturn(USER_ID);
  }

  private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req) {
    return req.header("Authorization", TOKEN);
  }

  private OrderVO vo() {
    OrderVO v = new OrderVO();
    v.setId(100L);
    v.setOrderNo("ORD1");
    v.setSeatId(1L);
    v.setOrderStatus(0);
    v.setPayAmount(new BigDecimal("20.00"));
    v.setBookingStart(LocalDateTime.now().plusHours(1));
    v.setBookingEnd(LocalDateTime.now().plusHours(3));
    return v;
  }

  /** 构造未来时段的下单请求体（避免 @Future 校验因写死过去时间而失败，测试随时可跑）。 */
  private String orderBody() {
    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    String start = LocalDateTime.now().plusHours(2).format(f);
    String end = LocalDateTime.now().plusHours(4).format(f);
    return "{\"seatId\":1,\"storeId\":10,\"bookingStart\":\"" + start + "\",\"bookingEnd\":\"" + end + "\"}";
  }

  @Test
  void create_带token返回订单() throws Exception {
    when(orderService.createOrder(any(), any())).thenReturn(vo());

    mvc.perform(withAuth(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderBody())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.orderNo").value("ORD1"));
  }

  @Test
  void cancel_返回成功() throws Exception {
    mvc.perform(withAuth(post("/api/order/100/cancel")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
    verify(orderService).cancelOrder(anyLong(), eq(100L), eq("用户主动取消"));
  }
}
