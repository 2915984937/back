package com.tencent.wxcloudrun.wx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信开放能力客户端：用 code 换取 openid / 手机号。
 * appid/secret 来自环境变量 WX_APPID / WX_SECRET。
 */
@Component
public class WxApiClient {

  private static final Logger log = LoggerFactory.getLogger(WxApiClient.class);

  private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
  private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
  private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public WxApiClient() {
    this.restTemplate = new RestTemplate();
  }

  WxApiClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Value("${app.wx.appid:}")
  String appid;

  @Value("${app.wx.secret:}")
  String secret;

  private volatile String cachedAccessToken;
  private volatile long tokenExpireAt;

  /** 用前端 wx.login() 的 code 换 openid */
  public String code2Session(String code) {
    ensureConfig();
    if (!StringUtils.hasText(code)) {
      throw new BizException(1001, "缺少微信登录 code");
    }

    String url = CODE2SESSION_URL + "?appid=" + appid
        + "&secret=" + secret
        + "&js_code=" + code
        + "&grant_type=authorization_code";

    JsonNode node = getJson(url);
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

  /** 用 getPhoneNumber 的 code 换手机号 */
  public String getPhoneNumber(String phoneCode) {
    ensureConfig();
    if (!StringUtils.hasText(phoneCode)) {
      throw new BizException(1001, "缺少手机号授权 code");
    }

    String accessToken = getAccessToken();
    String url = PHONE_NUMBER_URL + "?access_token=" + accessToken;

    Map<String, String> body = new HashMap<>();
    body.put("code", phoneCode);

    String raw = restTemplate.postForObject(url, body, String.class);
    JsonNode node = parseJson(raw);
    if (node.has("errcode") && node.get("errcode").asInt() != 0) {
      log.warn("wx getPhoneNumber返回错误: {}", node);
      throw new BizException(1001, "获取手机号失败：" + node.path("errmsg").asText(""));
    }
    JsonNode phoneInfo = node.path("phone_info");
    String pure = phoneInfo.path("purePhoneNumber").asText(null);
    if (!StringUtils.hasText(pure)) {
      throw new BizException(1001, "微信未返回手机号");
    }
    return pure;
  }

  /** GET: 拿 String 再手动 parse —— 不依赖 Content-Type */
  private JsonNode getJson(String url) {
    String raw = restTemplate.getForObject(url, String.class);
    return parseJson(raw);
  }

  private JsonNode parseJson(String raw) {
    if (raw == null) {
      throw new BizException(1001, "微信接口响应为空");
    }
    try {
      return objectMapper.readTree(raw);
    } catch (Exception e) {
      log.error("解析微信 JSON 失败: {}", raw, e);
      throw new BizException(1001, "微信接口响应格式异常");
    }
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
      JsonNode node = getJson(url);
      if (node.has("errcode") && node.get("errcode").asInt() != 0) {
        log.warn("wx access_token 返回错误: {}", node);
        throw new BizException(1001, "获取微信 access_token 失败：" + node.path("errmsg").asText(""));
      }
      cachedAccessToken = node.path("access_token").asText(null);
      int expiresIn = node.path("expires_in").asInt(7200);
      tokenExpireAt = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
      if (!StringUtils.hasText(cachedAccessToken)) {
        throw new BizException(1001, "微信未返回 access_token");
      }
      return cachedAccessToken;
    }
  }
}