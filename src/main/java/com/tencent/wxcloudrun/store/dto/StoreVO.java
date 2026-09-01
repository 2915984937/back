package com.tencent.wxcloudrun.store.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店返回 VO（C 端）。
 */
public class StoreVO {

  private Long id;
  private String name;
  private String address;
  private BigDecimal longitude;
  private BigDecimal latitude;
  private String coverImage;
  private String phone;
  private String layoutImage;
  private Integer status;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  private Double distance;     // 与用户的距离，单位 km，非数据库字段

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

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

  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

  public Double getDistance() { return distance; }
  public void setDistance(Double distance) { this.distance = distance; }
}
