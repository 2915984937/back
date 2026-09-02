package com.tencent.wxcloudrun.booking.dto;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 创建订单请求：用户选定座位 + 起止时间。
 * 防双订的权威判断在服务端（锁座位行 + 重叠查询），前端只传参。
 */
public class OrderCreateRequest {

  @NotNull(message = "请选择座位")
  private Long seatId;

  @NotNull(message = "请选择门店")
  private Long storeId;

  @NotNull(message = "请选择开始时间")
  @Future(message = "开始时间需晚于当前时间")
  private LocalDateTime bookingStart;

  @NotNull(message = "请选择结束时间")
  @Future(message = "结束时间需晚于当前时间")
  private LocalDateTime bookingEnd;

  public Long getSeatId() { return seatId; }
  public void setSeatId(Long seatId) { this.seatId = seatId; }

  public Long getStoreId() { return storeId; }
  public void setStoreId(Long storeId) { this.storeId = storeId; }

  public LocalDateTime getBookingStart() { return bookingStart; }
  public void setBookingStart(LocalDateTime bookingStart) { this.bookingStart = bookingStart; }

  public LocalDateTime getBookingEnd() { return bookingEnd; }
  public void setBookingEnd(LocalDateTime bookingEnd) { this.bookingEnd = bookingEnd; }
}
