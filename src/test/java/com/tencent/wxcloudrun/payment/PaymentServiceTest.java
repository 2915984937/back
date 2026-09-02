package com.tencent.wxcloudrun.payment;

import com.tencent.wxcloudrun.booking.model.Order;
import com.tencent.wxcloudrun.booking.service.OrderService;
import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.payment.dto.PrepayRequest;
import com.tencent.wxcloudrun.payment.service.PaymentService;
import com.tencent.wxcloudrun.user.dao.UserMapper;
import com.tencent.wxcloudrun.user.model.User;
import com.tencent.wxcloudrun.wx.WxPayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock OrderService orderService;
  @Mock WxPayClient wxPayClient;
  @Mock UserMapper userMapper;

  @InjectMocks PaymentService service;

  @Test
  void 预下单_返回微信支付参数() {
    Order o = new Order();
    o.setId(1L);
    o.setOrderNo("ORD1");
    o.setUserId(42L);
    o.setOrderStatus(0);
    o.setPayAmount(new BigDecimal("20.00"));
    when(orderService.requireOwnedOrder(42L, 1L)).thenReturn(o);

    User u = new User();
    u.setOpenid("openid-abc");
    when(userMapper.selectById(42L)).thenReturn(u);

    Map<String, String> payParams = new HashMap<>();
    payParams.put("package", "prepay_id=xyz");
    when(wxPayClient.unifiedOrder(eq("ORD1"), eq(new BigDecimal("20.00")), eq("openid-abc")))
        .thenReturn(payParams);

    Map<String, String> result = service.createPrepay(42L, 1L);

    assertEquals("prepay_id=xyz", result.get("package"));
    verify(wxPayClient).unifiedOrder(anyString(), any(), anyString());
  }

  @Test
  void 回调_验签通过则confirmPaid并返回SUCCESS() {
    Map<String, String> notify = new HashMap<>();
    notify.put("out_trade_no", "ORD1");
    notify.put("transaction_id", "TXN1");
    notify.put("total_fee", "2000");   // 分
    when(wxPayClient.parseNotify("<xml/>")).thenReturn(notify);

    String resp = service.handleNotify("<xml/>");

    assertEquals("SUCCESS", resp);
    verify(orderService).confirmPaid("ORD1", "TXN1", new BigDecimal("20.00"));
  }

  @Test
  void 回调_验签失败则返回FAIL() {
    when(wxPayClient.parseNotify(anyString())).thenThrow(new BizException(1001, "签名错误"));

    String resp = service.handleNotify("<xml/>");

    assertEquals("FAIL", resp);
    verify(orderService, never()).confirmPaid(anyString(), anyString(), any());
  }
}
