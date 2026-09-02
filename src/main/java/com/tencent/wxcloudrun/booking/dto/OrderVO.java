package com.tencent.wxcloudrun.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象。
 */
public class OrderVO {

  private Long id;
  private String orderNo;
  private Long userId;
  private Long storeId;
  private Long seatId;
  private String seatName;
  private LocalDateTime bookingStart;
  private LocalDateTime bookingEnd;
  private Integer durationMin;
  private BigDecimal payAmount;
  private Integer orderStatus;
  private String cancelReason;
  private LocalDateTime expireAt;
  private LocalDateTime payTime;
  private LocalDateTime actualStart;
  private LocalDateTime actualEnd;
  private LocalDateTime createTime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getOrderNo() { return orderNo; }
  public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }

  public Long getStoreId() { return storeId; }
  public void setStoreId(Long storeId) { this.storeId = storeId; }

  public Long getSeatId() { return seatId; }
  public void setSeatId(Long seatId) { this.seatId = seatId; }

  public String getSeatName() { return seatName; }
  public void setSeatName(String seatName) { this.seatName = seatName; }

  public LocalDateTime getBookingStart() { return bookingStart; }
  public void setBookingStart(LocalDateTime bookingStart) { this.bookingStart = bookingStart; }

  public LocalDateTime getBookingEnd() { return bookingEnd; }
  public void setBookingEnd(LocalDateTime bookingEnd) { this.bookingEnd = bookingEnd; }

  public Integer getDurationMin() { return durationMin; }
  public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }

  public BigDecimal getPayAmount() { return payAmount; }
  public void setPayAmount(BigDecimal payAmount) { this.payAmount = payAmount; }

  public Integer getOrderStatus() { return orderStatus; }
  public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }

  public String getCancelReason() { return cancelReason; }
  public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

  public LocalDateTime getExpireAt() { return expireAt; }
  public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }

  public LocalDateTime getPayTime() { return payTime; }
  public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }

  public LocalDateTime getActualStart() { return actualStart; }
  public void setActualStart(LocalDateTime actualStart) { this.actualStart = actualStart; }

  public LocalDateTime getActualEnd() { return actualEnd; }
  public void setActualEnd(LocalDateTime actualEnd) { this.actualEnd = actualEnd; }

  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
