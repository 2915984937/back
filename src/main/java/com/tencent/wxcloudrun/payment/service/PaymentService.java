package com.tencent.wxcloudrun.payment.service;

import com.tencent.wxcloudrun.booking.model.Order;
import com.tencent.wxcloudrun.booking.service.OrderService;
import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.user.dao.UserMapper;
import com.tencent.wxcloudrun.user.model.User;
import com.tencent.wxcloudrun.wx.WxPayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 支付服务：预下单（返回小程序支付参数）+ 微信回调处理。
 *
 * 回调把验签后的字段交给 OrderService.confirmPaid 落库（行锁 + 状态再审 + 原路退款），
 * 本类不直接改订单状态，保持职责单一。
 */
@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  private final OrderService orderService;
  private final WxPayClient wxPayClient;
  private final UserMapper userMapper;

  public PaymentService(OrderService orderService, WxPayClient wxPayClient, UserMapper userMapper) {
    this.orderService = orderService;
    this.wxPayClient = wxPayClient;
    this.userMapper = userMapper;
  }

  /** 预下单：校验订单归属与状态后，调用微信统一下单，返回 wx.requestPayment 参数。 */
  public Map<String, String> createPrepay(Long userId, Long orderId) {
    Order order = orderService.requireOwnedOrder(userId, orderId);
    if (order.getOrderStatus() != 0) {
      throw new BizException(400, "订单不可支付（已支付或已取消）");
    }
    User user = userMapper.selectById(userId);
    if (user == null || user.getOpenid() == null || user.getOpenid().isEmpty()) {
      throw new BizException(1004, "用户微信信息缺失，无法发起支付");
    }
    return wxPayClient.unifiedOrder(order.getOrderNo(), order.getPayAmount(), user.getOpenid());
  }

  /**
   * 处理微信支付回调。返回 "SUCCESS"/"FAIL" 供直接回写微信。
   * 任何异常都返回 FAIL，让微信按策略重试；成功则回 SUCCESS。
   */
  public String handleNotify(String xml) {
    try {
      Map<String, String> notify = wxPayClient.parseNotify(xml);
      String orderNo = notify.get("out_trade_no");
      String transactionId = notify.get("transaction_id");
      String feeFen = notify.get("total_fee");
      BigDecimal amount = new BigDecimal(feeFen == null ? "0" : feeFen)
          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      orderService.confirmPaid(orderNo, transactionId, amount);
      return "SUCCESS";
    } catch (Exception e) {
      log.error("微信支付回调处理失败", e);
      return "FAIL";
    }
  }
}
