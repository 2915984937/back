package com.tencent.wxcloudrun.wx;

import com.tencent.wxcloudrun.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WxApiClientTest {

  @Mock RestTemplate restTemplate;
  @InjectMocks WxApiClient wxApiClient;

  @BeforeEach
  void setUp() {
    wxApiClient.appid = "wx-test-appid";
    wxApiClient.secret = "test-secret";
  }

  @Test
  void code2Session_成功返回openid() {
    when(restTemplate.getForObject(anyString(), eq(String.class)))
        .thenReturn("{\"openid\":\"o_test_123\",\"session_key\":\"sk\"}");

    String openid = wxApiClient.code2Session("c001");
    assertEquals("o_test_123", openid);
  }

  @Test
  void code2Session_微信返回错误_抛BizException() {
    when(restTemplate.getForObject(anyString(), eq(String.class)))
        .thenReturn("{\"errcode\":40029,\"errmsg\":\"invalid code\"}");

    BizException ex = assertThrows(BizException.class,
        () -> wxApiClient.code2Session("bad"));
    assertEquals(1001, ex.getCode());
    assertTrue(ex.getMessage().contains("invalid code"));
  }

  @Test
  void code2Session_响应为空_抛BizException() {
    when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

    BizException ex = assertThrows(BizException.class,
        () -> wxApiClient.code2Session("c001"));
    assertEquals(1001, ex.getCode());
  }

  @Test
  void getPhoneNumber_成功返回纯数字手机号() {
    when(restTemplate.getForObject(anyString(), eq(String.class)))
        .thenReturn("{\"access_token\":\"at_test\",\"expires_in\":7200}");
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
        .thenReturn("{\"errcode\":0,\"errmsg\":\"ok\",\"phone_info\":{\"purePhoneNumber\":\"13800138000\"}}");

    String phone = wxApiClient.getPhoneNumber("phone-code");
    assertEquals("13800138000", phone);
  }

  @Test
  void getPhoneNumber_微信返回错误_抛BizException() {
    when(restTemplate.getForObject(anyString(), eq(String.class)))
        .thenReturn("{\"access_token\":\"at_test\",\"expires_in\":7200}");
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
        .thenReturn("{\"errcode\":40003,\"errmsg\":\"invalid code\"}");

    BizException ex = assertThrows(BizException.class,
        () -> wxApiClient.getPhoneNumber("bad"));
    assertEquals(1001, ex.getCode());
  }

  @Test
  void 未配置appid_抛BizException() {
    wxApiClient.appid = "";
    wxApiClient.secret = "";

    BizException ex = assertThrows(BizException.class,
        () -> wxApiClient.code2Session("c001"));
    assertEquals(1001, ex.getCode());
    assertTrue(ex.getMessage().contains("未配置"));
  }

  @Test
  void code为空_抛BizException() {
    assertThrows(BizException.class, () -> wxApiClient.code2Session(""));
    assertThrows(BizException.class, () -> wxApiClient.code2Session(null));
  }
}