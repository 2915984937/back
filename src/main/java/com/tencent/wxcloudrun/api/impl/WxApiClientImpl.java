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

/**
 * 通过微信 jscode2session 接口用 code 换取 openid。
 * appid/secret 来自环境变量 WX_APPID / WX_SECRET（微信云托管环境变量）。
 */
@Component
public class WxApiClientImpl implements WxApiClient {

  private static final Logger log = LoggerFactory.getLogger(WxApiClientImpl.class);

  private static final String URL = "https://api.weixin.qq.com/sns/jscode2session";

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${app.wx.appid:}")
  private String appid;

  @Value("${app.wx.secret:}")
  private String secret;

  @Override
  public String code2Session(String code) {
    if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
      log.error("服务端未配置 app.wx.appid / app.wx.secret（环境变量 WX_APPID / WX_SECRET）");
      throw new BizException(1001, "服务端未配置微信登录参数");
    }
    if (!StringUtils.hasText(code)) {
      throw new BizException(1001, "缺少微信登录 code");
    }

    String url = URL + "?appid=" + appid
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
}