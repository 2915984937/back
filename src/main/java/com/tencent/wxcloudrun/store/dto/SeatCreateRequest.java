package com.tencent.wxcloudrun.store.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 座位创建/更新请求（管理后台）。
 */
public class SeatCreateRequest {

  @NotNull(message = "门店ID不能为空")
  private Long storeId;

  @NotBlank(message = "座位名称不能为空")
  private String seatName;

  @NotNull(message = "X 坐标不能为空")
  private Integer posX;

  @NotNull(message = "Y 坐标不能为空")
  private Integer posY;

  @NotNull(message = "小时价格不能为空")
  @DecimalMin(value = "0.00", message = "小时价格不能小于 0")
  private BigDecimal priceHour;

  @NotNull(message = "状态不能为空")
  private Integer status;      // 0-可用 1-维护 2-停用

  private String deviceId;

  public Long getStoreId() { return storeId; }
  public void setStoreId(Long storeId) { this.storeId = storeId; }

  public String getSeatName() { return seatName; }
  public void setSeatName(String seatName) { this.seatName = seatName; }

  public Integer getPosX() { return posX; }
  public void setPosX(Integer posX) { this.posX = posX; }

  public Integer getPosY() { return posY; }
  public void setPosY(Integer posY) { this.posY = posY; }

  public BigDecimal getPriceHour() { return priceHour; }
  public void setPriceHour(BigDecimal priceHour) { this.priceHour = priceHour; }

  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }

  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
