package com.tencent.wxcloudrun.booking;

import com.tencent.wxcloudrun.booking.dao.OrderMapper;
import com.tencent.wxcloudrun.booking.dto.OrderCreateRequest;
import com.tencent.wxcloudrun.booking.dto.OrderVO;
import com.tencent.wxcloudrun.booking.model.Order;
import com.tencent.wxcloudrun.booking.service.OrderService;
import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.mqtt.MqttPublisher;
import com.tencent.wxcloudrun.payment.dao.PaymentRecordMapper;
import com.tencent.wxcloudrun.store.dao.SeatMapper;
import com.tencent.wxcloudrun.store.dao.StoreMapper;
import com.tencent.wxcloudrun.store.model.Seat;
import com.tencent.wxcloudrun.store.model.Store;
import com.tencent.wxcloudrun.wx.WxPayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock OrderMapper orderMapper;
  @Mock SeatMapper seatMapper;
  @Mock StoreMapper storeMapper;
  @Mock PaymentRecordMapper paymentRecordMapper;
  @Mock WxPayClient wxPayClient;
  @Mock MqttPublisher mqttPublisher;

  @InjectMocks OrderService service;

  private Seat seat(long id, BigDecimal price) {
    Seat s = new Seat();
    s.setId(id);
    s.setSeatName("A01");
    s.setPriceHour(price);
    s.setStatus(0);          // 可用
    s.setDeviceId("dev-001");
    return s;
  }

  private Store store(long id) {
    Store s = new Store();
    s.setId(id);
    s.setStatus(0);
    return s;
  }

  private OrderCreateRequest req(long seatId, long storeId, LocalDateTime start, LocalDateTime end) {
    OrderCreateRequest r = new OrderCreateRequest();
    r.setSeatId(seatId);
    r.setStoreId(storeId);
    r.setBookingStart(start);
    r.setBookingEnd(end);
    return r;
  }

  @Test
  void 创建订单_锁座加重叠检查后落库并计算金额() {
    LocalDateTime start = LocalDateTime.now().plusHours(1);
    LocalDateTime end = start.plusHours(2);   // 2 小时，单价 10 → 20 元
    when(seatMapper.selectById(1L)).thenReturn(seat(1L, new BigDecimal("10.00")));
    when(storeMapper.selectById(10L)).thenReturn(store(10L));
    when(orderMapper.existsOverlap(eq(1L), any(), any())).thenReturn(0);
    when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
      Order o = inv.getArgument(0);
      o.setId(100L);
      return 1;
    });

    OrderVO vo = service.createOrder(42L, req(1L, 10L, start, end));

    assertNotNull(vo);
    assertEquals(0, vo.getOrderStatus());
    assertEquals(new BigDecimal("20.00"), vo.getPayAmount());
    assertEquals(120, vo.getDurationMin());
    verify(orderMapper).selectSeatForUpdate(1L);
    verify(orderMapper).insert(any(Order.class));
  }

  @Test
  void 创建订单_时段重叠则拒绝() {
    LocalDateTime start = LocalDateTime.now().plusHours(1);
    when(seatMapper.selectById(1L)).thenReturn(seat(1L, new BigDecimal("10.00")));
    when(storeMapper.selectById(10L)).thenReturn(store(10L));
    when(orderMapper.existsOverlap(eq(1L), any(), any())).thenReturn(1);

    BizException ex = assertThrows(BizException.class,
        () -> service.createOrder(42L, req(1L, 10L, start, start.plusHours(2))));
    assertTrue(ex.getMessage().contains("已被预约"));
    verify(orderMapper, never()).insert(any());
  }

  @Test
  void 创建订单_座位不可用抛异常() {
    Seat s = seat(1L, new BigDecimal("10.00"));
    s.setStatus(1);  // 维护中
    when(seatMapper.selectById(1L)).thenReturn(s);

    assertThrows(BizException.class,
        () -> service.createOrder(42L, req(1L, 10L, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2))));
  }

  @Test
  void 取消订单_待支付可取消并关单() {
    Order o = new Order();
    o.setId(100L);
    o.setOrderNo("ORD123");
    o.setUserId(42L);
    o.setOrderStatus(0);
    when(orderMapper.selectById(100L)).thenReturn(o);

    service.cancelOrder(42L, 100L, "用户主动取消");

    verify(wxPayClient).closeOrder("ORD123");
    verify(orderMapper).updateStatusCancelled(eq(100L), anyString());
  }

  @Test
  void 取消订单_非本人无权() {
    Order o = new Order();
    o.setId(100L);
    o.setUserId(99L);
    o.setOrderStatus(0);
    when(orderMapper.selectById(100L)).thenReturn(o);

    assertThrows(BizException.class, () -> service.cancelOrder(42L, 100L, "x"));
  }

  @Test
  void 回调confirmPaid_待支付转待用并落支付记录() {
    Order o = new Order();
    o.setId(100L);
    o.setOrderNo("ORD1");
    o.setUserId(42L);
    o.setOrderStatus(0);
    when(orderMapper.selectForUpdateByOrderNo("ORD1")).thenReturn(o);

    service.confirmPaid("ORD1", "TXN1", new BigDecimal("20.00"));

    verify(paymentRecordMapper).insert(any());
    verify(orderMapper).updateStatusPaid(eq(100L), any());
  }

  @Test
  void 回调confirmPaid_已支付则幂等不重复落库() {
    Order o = new Order();
    o.setId(100L);
    o.setOrderNo("ORD1");
    o.setOrderStatus(1);   // 已支付
    when(orderMapper.selectForUpdateByOrderNo("ORD1")).thenReturn(o);

    service.confirmPaid("ORD1", "TXN1", new BigDecimal("20.00"));

    verify(paymentRecordMapper, never()).insert(any());
    verify(orderMapper, never()).updateStatusPaid(anyLong(), any());
  }

  @Test
  void 回调confirmPaid_已取消却收款则原路退款() {
    Order o = new Order();
    o.setId(100L);
    o.setOrderNo("ORD1");
    o.setUserId(42L);
    o.setOrderStatus(4);   // 已取消
    when(orderMapper.selectForUpdateByOrderNo("ORD1")).thenReturn(o);

    service.confirmPaid("ORD1", "TXN1", new BigDecimal("20.00"));

    verify(wxPayClient).refund(eq("ORD1"), eq("TXN1"), eq(new BigDecimal("20.00")));
    verify(paymentRecordMapper).insert(any());
    verify(orderMapper).updateStatusRefunded(100L);
  }

  @Test
  void 超时扫描_关单并置取消() {
    Order o = new Order();
    o.setId(100L);
    o.setOrderNo("ORD1");
    o.setOrderStatus(0);
    when(orderMapper.selectExpiredUnpaid(any(), anyInt())).thenReturn(Collections.singletonList(o));

    service.expireScan();

    verify(wxPayClient).closeOrder("ORD1");
    verify(orderMapper).updateStatusCancelled(eq(100L), contains("超时"));
  }
}
