package com.tencent.wxcloudrun.payment.dto;

import javax.validation.constraints.NotNull;

/**
 * 预下单请求：携带待支付订单 ID。
 */
public class PrepayRequest {

  @NotNull(message = "请指定订单")
  private Long orderId;

  public Long getOrderId() { return orderId; }
  public void setOrderId(Long orderId) { this.orderId = orderId; }
}
