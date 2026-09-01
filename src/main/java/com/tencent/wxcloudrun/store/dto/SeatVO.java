package com.tencent.wxcloudrun.store.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 座位返回 VO（C 端）。
 */
public class SeatVO {

  private Long id;
  private Long storeId;
  private String seatName;
  private Integer posX;
  private Integer posY;
  private Integer seatType;       // 1-单人 2-双人 3-VIP
  private BigDecimal priceHour;
  private Integer status;           // 0-可用 1-维护 2-停用
  private String deviceId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getStoreId() { return storeId; }
  public void setStoreId(Long storeId) { this.storeId = storeId; }

  public String getSeatName() { return seatName; }
  public void setSeatName(String seatName) { this.seatName = seatName; }

  public Integer getPosX() { return posX; }
  public void setPosX(Integer posX) { this.posX = posX; }

  public Integer getPosY() { return posY; }
  public void setPosY(Integer posY) { this.posY = posY; }

  public Integer getSeatType() { return seatType; }
  public void setSeatType(Integer seatType) { this.seatType = seatType; }

  public BigDecimal getPriceHour() { return priceHour; }
  public void setPriceHour(BigDecimal priceHour) { this.priceHour = priceHour; }

  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }

  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
