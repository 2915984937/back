package com.tencent.wxcloudrun.store;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.store.dao.SeatMapper;
import com.tencent.wxcloudrun.store.dao.StoreMapper;
import com.tencent.wxcloudrun.store.dto.StoreCreateRequest;
import com.tencent.wxcloudrun.store.dto.StoreListRequest;
import com.tencent.wxcloudrun.store.dto.StoreVO;
import com.tencent.wxcloudrun.store.model.Store;
import com.tencent.wxcloudrun.store.service.StoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

  @Mock StoreMapper storeMapper;
  @Mock SeatMapper seatMapper;
  @InjectMocks StoreService service;

  private Store store(Long id, String name, double lat, double lon) {
    Store s = new Store();
    s.setId(id);
    s.setName(name);
    s.setAddress("addr" + id);
    s.setLatitude(BigDecimal.valueOf(lat));
    s.setLongitude(BigDecimal.valueOf(lon));
    s.setStatus(0);
    return s;
  }

  @Test
  void 列表_默认只查营业中() {
    StoreListRequest req = new StoreListRequest();
    when(storeMapper.selectList(any())).thenReturn(Collections.singletonList(store(1L, "A", 30.6, 104.0)));

    List<StoreVO> list = service.list(req);

    assertEquals(1, list.size());
    assertEquals(0, req.getStatus());
  }

  @Test
  void 列表_带距离按近到远排序() {
    StoreListRequest req = new StoreListRequest();
    req.setLatitude(BigDecimal.valueOf(30.60));
    req.setLongitude(BigDecimal.valueOf(104.06));
    Store a = store(1L, "近", 30.605, 104.065);
    Store b = store(2L, "远", 30.62, 104.08);
    when(storeMapper.selectList(any())).thenReturn(Arrays.asList(b, a));

    List<StoreVO> list = service.list(req);

    assertEquals("近", list.get(0).getName());
    assertTrue(list.get(0).getDistance() < list.get(1).getDistance());
  }

  @Test
  void 详情_不存在抛异常() {
    when(storeMapper.selectById(99L)).thenReturn(null);
    assertThrows(BizException.class, () -> service.detail(99L));
  }

  @Test
  void 创建_参数落地并返回VO() {
    StoreCreateRequest req = new StoreCreateRequest();
    req.setName("  华阳店  ");
    req.setAddress("天府大道");
    req.setLatitude(BigDecimal.valueOf(30.6));
    req.setLongitude(BigDecimal.valueOf(104.0));
    req.setStatus(0);

    when(storeMapper.insert(any(Store.class))).thenAnswer(inv -> {
      Store s = inv.getArgument(0);
      s.setId(10L);
      return 1;
    });

    StoreVO vo = service.create(req);

    assertEquals(10L, vo.getId());
    assertEquals("华阳店", vo.getName()); // trim
    verify(storeMapper).insert(any(Store.class));
  }

  @Test
  void 删除_连带删除座位() {
    when(storeMapper.selectById(1L)).thenReturn(store(1L, "A", 0, 0));

    service.delete(1L);

    verify(seatMapper).deleteByStoreId(1L);
    verify(storeMapper).deleteById(1L);
  }
}
