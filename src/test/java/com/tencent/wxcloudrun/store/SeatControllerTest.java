package com.tencent.wxcloudrun.store;

import com.tencent.wxcloudrun.common.util.JwtUtil;
import com.tencent.wxcloudrun.store.dto.SeatVO;
import com.tencent.wxcloudrun.store.service.SeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.tencent.wxcloudrun.store.controller.SeatController.class)
class SeatControllerTest {

  private static final Long USER_ID = 42L;
  private static final String TOKEN = "Bearer valid.token.here";

  @Autowired MockMvc mvc;
  @MockBean SeatService seatService;
  @MockBean JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    when(jwtUtil.parseUserId(anyString())).thenReturn(USER_ID);
  }

  private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req) {
    return req.header("Authorization", TOKEN);
  }

  private SeatVO vo(Long id) {
    SeatVO v = new SeatVO();
    v.setId(id);
    v.setStoreId(1L);
    v.setSeatName("A01");
    v.setPosX(100);
    v.setPosY(200);
    v.setPriceHour(BigDecimal.valueOf(8.00));
    v.setStatus(0);
    return v;
  }

  @Test
  void create_新增座位() throws Exception {
    when(seatService.create(any())).thenReturn(vo(1L));

    mvc.perform(withAuth(post("/api/store/admin/seat")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"storeId\":1,\"seatName\":\"A01\",\"posX\":100,\"posY\":200,\"priceHour\":8.00,\"status\":0}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.seatName").value("A01"));
  }

  @Test
  void update_修改座位() throws Exception {
    when(seatService.update(eq(1L), any())).thenReturn(vo(1L));

    mvc.perform(withAuth(put("/api/store/admin/seat/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"storeId\":1,\"seatName\":\"A02\",\"posX\":120,\"posY\":220,\"priceHour\":10.00,\"status\":0}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  void delete_删除座位() throws Exception {
    mvc.perform(withAuth(delete("/api/store/admin/seat/1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }
}
