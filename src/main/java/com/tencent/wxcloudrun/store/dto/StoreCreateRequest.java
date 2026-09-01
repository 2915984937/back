package com.tencent.wxcloudrun.store.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 门店创建/更新请求（管理后台）。
 */
public class StoreCreateRequest {

  @NotBlank(message = "门店名称不能为空")
  private String name;

  @NotBlank(message = "详细地址不能为空")
  private String address;

  @NotNull(message = "经度不能为空")
  @DecimalMin(value = "-180.0", message = "经度范围错误")
  @DecimalMax(value = "180.0", message = "经度范围错误")
  private BigDecimal longitude;

  @NotNull(message = "纬度不能为空")
  @DecimalMin(value = "-90.0", message = "纬度范围错误")
  @DecimalMax(value = "90.0", message = "纬度范围错误")
  private BigDecimal latitude;

  private String coverImage;

  private String phone;

  private String layoutImage;

  @NotNull(message = "状态不能为空")
  private Integer status;        // 0-营业 1-暂停 2-关闭

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }

  public BigDecimal getLongitude() { return longitude; }
  public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

  public BigDecimal getLatitude() { return latitude; }
  public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

  public String getCoverImage() { return coverImage; }
  public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }

  public String getLayoutImage() { return layoutImage; }
  public void setLayoutImage(String layoutImage) { this.layoutImage = layoutImage; }

  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
}
