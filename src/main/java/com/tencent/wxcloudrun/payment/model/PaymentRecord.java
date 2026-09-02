package com.tencent.wxcloudrun.payment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录（biz_payment_record）。
 *
 * 幂等：transaction_id（微信交易流水号）唯一，重复回调第二次插不进去 → 自然只处理一次。
 * 仅微信支付：channel 字段已移除，金额币种为人民币。
 */
public class PaymentRecord {

  /** 支付成功 */
  public static final int STATUS_SUCCESS = 1;
  /** 已退款（订单取消后款项原路退回） */
  public static final int STATUS_REFUNDED = 2;

  private Long id;
  private Long orderId;
  private String orderNo;
  private String payNo;
  private String transactionId;
  private BigDecimal amount;
  private Integer payStatus;
  private String callbackData;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getOrderId() { return orderId; }
  public void setOrderId(Long orderId) { this.orderId = orderId; }

  public String getOrderNo() { return orderNo; }
  public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

  public String getPayNo() { return payNo; }
  public void setPayNo(String payNo) { this.payNo = payNo; }

  public String getTransactionId() { return transactionId; }
  public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }

  public Integer getPayStatus() { return payStatus; }
  public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }

  public String getCallbackData() { return callbackData; }
  public void setCallbackData(String callbackData) { this.callbackData = callbackData; }

  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
