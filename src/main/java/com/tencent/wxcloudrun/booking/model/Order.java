package com.tencent.wxcloudrun.booking.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约订单（biz_order）。
 *
 * 状态机 order_status：
 *   0 待支付 → 1 待用 → 2 使用中 → 3 完成
 *   4 取消（超时/用户主动）   5 退款（已付后取消，原路退回）
 *
 * 设计要点（对齐概要设计.md §156/§158，且无 Redis）：
 *   - 防双订：创建时锁 biz_seat 行 + 查 biz_order 自身重叠区间（见 OrderMapper.existsOverlap）
 *   - 超时取消：expire_at + idx_status_expire，由 @Scheduled 每分钟扫描
 *   - 取消即 closeorder；回调行锁状态再审；已取消却收款 → 原路退款
 */
public class Order {

  private Long id;
  private String orderNo;
  private Long userId;
  private Long storeId;
  private Long seatId;
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
  private LocalDateTime updateTime;

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

  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

  /** 是否已取消（含退款）。 */
  public boolean isCancelled() {
    return orderStatus != null && (orderStatus == 4 || orderStatus == 5);
  }
}
