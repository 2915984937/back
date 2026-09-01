package com.tencent.wxcloudrun.store.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * 门店列表查询参数（C 端公开接口）。
 */
public class StoreListRequest {

  private String keyword;          // 名称/地址模糊搜索

  private Integer status;          // 0-营业 1-暂停 2-关闭，默认查营业中

  @DecimalMin(value = "-180.0", message = "经度范围错误")
  @DecimalMax(value = "180.0", message = "经度范围错误")
  private BigDecimal longitude;    // 用户当前经度，用于距离排序

  @DecimalMin(value = "-90.0", message = "纬度范围错误")
  @DecimalMax(value = "90.0", message = "纬度范围错误")
  private BigDecimal latitude;     // 用户当前纬度

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }

  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }

  public BigDecimal getLongitude() { return longitude; }
  public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

  public BigDecimal getLatitude() { return latitude; }
  public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
}
