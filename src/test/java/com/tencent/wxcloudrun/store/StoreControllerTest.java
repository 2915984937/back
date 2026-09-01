package com.tencent.wxcloudrun.store;

import com.tencent.wxcloudrun.common.util.JwtUtil;
import com.tencent.wxcloudrun.store.dto.StoreVO;
import com.tencent.wxcloudrun.store.service.SeatService;
import com.tencent.wxcloudrun.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.tencent.wxcloudrun.store.controller.StoreController.class)
class StoreControllerTest {

  private static final Long USER_ID = 42L;
  private static final String TOKEN = "Bearer valid.token.here";

  @Autowired MockMvc mvc;
  @MockBean StoreService storeService;
  @MockBean SeatService seatService;
  @MockBean JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    when(jwtUtil.parseUserId(anyString())).thenReturn(USER_ID);
  }

  private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req) {
    return req.header("Authorization", TOKEN);
  }

  private StoreVO vo(Long id, String name) {
    StoreVO v = new StoreVO();
    v.setId(id);
    v.setName(name);
    v.setAddress("addr");
    v.setLatitude(BigDecimal.valueOf(30.6));
    v.setLongitude(BigDecimal.valueOf(104.0));
    v.setStatus(0);
    return v;
  }

  @Test
  void list_带token返回门店列表() throws Exception {
    when(storeService.list(any())).thenReturn(Collections.singletonList(vo(1L, "华阳店")));

    mvc.perform(withAuth(get("/api/store/list")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0].name").value("华阳店"));
  }

  @Test
  void detail_返回门店详情() throws Exception {
    when(storeService.detail(1L)).thenReturn(vo(1L, "华阳店"));

    mvc.perform(withAuth(get("/api/store/1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("华阳店"));
  }

  @Test
  void seats_返回座位列表() throws Exception {
    when(seatService.listByStore(1L)).thenReturn(Collections.emptyList());

    mvc.perform(withAuth(get("/api/store/1/seats")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  void adminCreate_创建门店() throws Exception {
    when(storeService.create(any())).thenReturn(vo(1L, "华阳店"));

    mvc.perform(withAuth(post("/api/store/admin/store")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"华阳店\",\"address\":\"天府大道\",\"longitude\":104.0,\"latitude\":30.6,\"status\":0}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1));
  }
}
