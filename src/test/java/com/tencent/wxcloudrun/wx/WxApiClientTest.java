package com.tencent.wxcloudrun.wx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WxApiClientTest {

  @Spy WxApiClient client;
  RestTemplate mockRestTemplate;

  @BeforeEach
  void setUp() {
    mockRestTemplate = mock(RestTemplate.class);
    ReflectionTestUtils.setField(client, "restTemplate", mockRestTemplate);
    ReflectionTestUtils.setField(client, "appid", "wx-appid");
    ReflectionTestUtils.setField(client, "secret", "wx-secret");
  }

  @Test
  void code2Session_成功返回openid() throws Exception {
    JsonNode node = new ObjectMapper().readTree("{\"openid\":\"openid_xyz\",\"session_key\":\"sk\"}");
    when(mockRestTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(node);

    String openid = client.code2Session("c001");

    assertEquals("openid_xyz", openid);
    verify(mockRestTemplate).getForObject(anyString(), eq(JsonNode.class));
  }

  @Test
  void code2Session_微信返回错误_抛BizException() throws Exception {
    JsonNode err = new ObjectMapper().readTree("{\"errcode\":40029,\"errmsg\":\"invalid code\"}");
    when(mockRestTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(err);

    BizException ex = assertThrows(BizException.class, () -> client.code2Session("bad"));
    assertEquals(1001, ex.getCode());
    assertTrue(ex.getMessage().contains("invalid code"));
  }

  @Test
  void code2Session_响应为空_抛BizException() {
    when(mockRestTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(null);

    BizException ex = assertThrows(BizException.class, () -> client.code2Session("c"));
    assertEquals(1001, ex.getCode());
  }

  @Test
  void code2Session_缺少code_抛BizException() {
    BizException ex = assertThrows(BizException.class, () -> client.code2Session(""));
    assertEquals(1001, ex.getCode());
    verify(mockRestTemplate, never()).getForObject(anyString(), any());
  }

  @Test
  void getPhoneNumber_成功返回纯数字手机号() throws Exception {
    JsonNode tokenNode = new ObjectMapper().readTree(
        "{\"access_token\":\"at-1\",\"expires_in\":7200}");
    JsonNode phoneNode = new ObjectMapper().readTree(
        "{\"phone_info\":{\"purePhoneNumber\":\"13800138000\",\"phoneNumber\":\"+8613800138000\"}}");

    when(mockRestTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(tokenNode);
    when(mockRestTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(phoneNode);

    String phone = client.getPhoneNumber("pcode");

    assertEquals("13800138000", phone);
  }

  @Test
  void getPhoneNumber_微信返回错误_抛BizException() throws Exception {
    JsonNode tokenNode = new ObjectMapper().readTree(
        "{\"access_token\":\"at-1\",\"expires_in\":7200}");
    JsonNode err = new ObjectMapper().readTree(
        "{\"errcode\":40003,\"errmsg\":\"invalid code\"}");

    when(mockRestTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(tokenNode);
    when(mockRestTemplate.postForObject(anyString(), any(), eq(JsonNode.class))).thenReturn(err);

    BizException ex = assertThrows(BizException.class, () -> client.getPhoneNumber("pcode"));
    assertEquals(1001, ex.getCode());
  }

  @Test
  void 配置缺失_抛BizException() {
    ReflectionTestUtils.setField(client, "appid", "");
    ReflectionTestUtils.setField(client, "secret", "");

    BizException ex = assertThrows(BizException.class, () -> client.code2Session("c"));
    assertEquals(1001, ex.getCode());
  }
}