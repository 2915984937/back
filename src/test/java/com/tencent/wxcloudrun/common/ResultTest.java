package com.tencent.wxcloudrun.common;

import com.tencent.wxcloudrun.common.util.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

  @Test
  void ok_携带数据() {
    Result<String> r = Result.ok("hello");
    assertThat(r.getCode()).isEqualTo(0);
    assertThat(r.getMessage()).isEqualTo("ok");
    assertThat(r.getData()).isEqualTo("hello");
    assertThat(r.getTimestamp()).isGreaterThan(0);
  }

  @Test
  void ok_可传null数据() {
    Result<Void> r = Result.ok(null);
    assertThat(r.getCode()).isEqualTo(0);
    assertThat(r.getData()).isNull();
  }

  @Test
  void error_带code和message() {
    Result<Void> r = Result.error(401, "未登录");
    assertThat(r.getCode()).isEqualTo(401);
    assertThat(r.getMessage()).isEqualTo("未登录");
    assertThat(r.getData()).isNull();
  }

  @Test
  void error_仅message时默认500() {
    Result<Void> r = Result.error("出问题了");
    assertThat(r.getCode()).isEqualTo(500);
    assertThat(r.getMessage()).isEqualTo("出问题了");
  }
}
