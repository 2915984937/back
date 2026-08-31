package com.tencent.wxcloudrun.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.wxcloudrun.api.WxApiClient;
import com.tencent.wxcloudrun.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 通过微信接口用 code 换取 openid / 手机号。
 * appid/secret 来自环境变量 WX_APPID / WX_SECRET（微信云托管环境变量）。
 */
@Component
public class WxApiClientImpl implements WxApiClient {

  private static final Logger log = LoggerFactory.getLogger(WxApiClientImpl.class);

  private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
  private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
  private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${app.wx.appid:}")
  private String appid;

  @Value("${app.wx.secret:}")
  private String secret;

  private volatile String cachedAccessToken;
  private volatile long tokenExpireAt;

  @Override
  public String code2Session(String code) {
    ensureConfig();
    if (!StringUtils.hasText(code)) {
      throw new BizException(1001, "缺少微信登录 code");
    }

    String url = CODE2SESSION_URL + "?appid=" + appid
        + "&secret=" + secret
        + "&js_code=" + code
        + "&grant_type=authorization_code";

    JsonNode node = restTemplate.getForObject(url, JsonNode.class);
    if (node == null) {
      throw new BizException(1001, "微信登录响应为空");
    }
    if (node.has("errcode") && node.get("errcode").asInt() != 0) {
      log.warn("wx code2session返回错误: {}", node);
      throw new BizException(1001, "微信登录失败：" + node.path("errmsg").asText(""));
    }
    String openid = node.path("openid").asText(null);
    if (!StringUtils.hasText(openid)) {
      throw new BizException(1001, "微信登录未返回 openid");
    }
    return openid;
  }

  @Override
  public String getPhoneNumber(String phoneCode) {
    ensureConfig();
    if (!StringUtils.hasText(phoneCode)) {
      throw new BizException(1001, "缺少手机号授权 code");
    }

    String accessToken = getAccessToken();
    String url = PHONE_NUMBER_URL + "?access_token=" + accessToken;

    Map<String, String> body = new HashMap<>();
    body.put("code", phoneCode);

    JsonNode node = restTemplate.postForObject(url, body, JsonNode.class);
    if (node == null) {
      throw new BizException(1001, "微信手机号接口响应为空");
    }
    if (node.has("errcode") && node.get("errcode").asInt() != 0) {
      log.warn("wx getPhoneNumber返回错误: {}", node);
      throw new BizException(1001, "获取手机号失败：" + node.path("errmsg").asText(""));
    }
    // 微信返回: { phone_info: { purePhoneNumber: "138xxxx1234", phoneNumber: "+86138xxxx1234" } }
    JsonNode phoneInfo = node.path("phone_info");
    String pure = phoneInfo.path("purePhoneNumber").asText(null);
    if (!StringUtils.hasText(pure)) {
      throw new BizException(1001, "微信未返回手机号");
    }
    return pure;
  }

  private void ensureConfig() {
    if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
      log.error("服务端未配置 app.wx.appid / app.wx.secret（环境变量 WX_APPID / WX_SECRET）");
      throw new BizException(1001, "服务端未配置微信登录参数");
    }
  }

  private String getAccessToken() {
    long now = System.currentTimeMillis();
    if (cachedAccessToken != null && now < tokenExpireAt) {
      return cachedAccessToken;
    }
    synchronized (this) {
      if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireAt) {
        return cachedAccessToken;
      }
      String url = ACCESS_TOKEN_URL + "?grant_type=client_credential"
          + "&appid=" + appid
          + "&secret=" + secret;
      JsonNode node = restTemplate.getForObject(url, JsonNode.class);
      if (node == null || node.has("errcode")) {
        log.warn("wx access_token 返回错误: {}", node);
        throw new BizException(1001, "获取微信 access_token 失败");
      }
      cachedAccessToken = node.path("access_token").asText(null);
      int expiresIn = node.path("expires_in").asInt(7200);
      // 提前 5 分钟过期，避免边界问题
      tokenExpireAt = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
      if (!StringUtils.hasText(cachedAccessToken)) {
        throw new BizException(1001, "微信未返回 access_token");
      }
      return cachedAccessToken;
    }
  }
}
