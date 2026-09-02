package com.tencent.wxcloudrun.wx;

import com.tencent.wxcloudrun.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付 v2（JSAPI）客户端：统一下单 / 关单 / 退款 / 回调验签。
 *
 * 与 WxApiClient 同风格（RestTemplate 直连，配置来自环境变量），无第三方 SDK 依赖。
 * 测试时整体 mock，不触网。
 *
 * 注意：refund 接口微信要求双向证书（apiclient_cert.p12），本实现未加载证书，
 * 生产环境需在 WxPayClient 构造函数注入 KeyStore 后才会真正成功，当前方法签名与流程已就位。
 */
@Component
public class WxPayClient {

  private static final Logger log = LoggerFactory.getLogger(WxPayClient.class);

  private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
  private static final String CLOSE_ORDER_URL   = "https://api.mch.weixin.qq.com/pay/closeorder";
  private static final String REFUND_URL         = "https://api.mch.weixin.qq.com/secapi/pay/refund";

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${app.wx.appid:}")
  String appid;

  @Value("${app.wx.pay.mch-id:}")
  String mchId;

  @Value("${app.wx.pay.mch-key:}")
  String mchKey;

  @Value("${app.wx.pay.notify-url:}")
  String notifyUrl;

  /** 统一下单（JSAPI），返回小程序 wx.requestPayment 所需的支付参数。 */
  public Map<String, String> unifiedOrder(String outTradeNo, BigDecimal amountYuan, String openid) {
    ensureConfig();
    if (outTradeNo == null || openid == null) {
      throw new BizException(400, "下单参数缺失");
    }
    Map<String, String> params = new HashMap<>();
    params.put("appid", appid);
    params.put("mch_id", mchId);
    params.put("nonce_str", genNonce());
    params.put("sign_type", "MD5");
    params.put("body", "自习室预约");
    params.put("out_trade_no", outTradeNo);
    params.put("total_fee", yuanToFen(amountYuan));
    params.put("spbill_create_ip", "127.0.0.1");
    params.put("notify_url", notifyUrl);
    params.put("trade_type", "JSAPI");
    params.put("openid", openid);
    params.put("sign", sign(params, mchKey));

    String resp = postXml(UNIFIED_ORDER_URL, mapToXml(params));
    Map<String, String> r = xmlToMap(resp);
    if (!"SUCCESS".equals(r.get("return_code")) || !"SUCCESS".equals(r.get("result_code"))) {
      log.warn("微信统一下单失败: {}", r);
      throw new BizException(1001, "微信统一下单失败：" + r.get("return_msg"));
    }
    return buildPayParams(r.get("prepay_id"));
  }

  /** 关单：未支付订单取消 / 超时取消时调用，让用户侧付不出去。 */
  public void closeOrder(String outTradeNo) {
    ensureConfig();
    Map<String, String> params = new HashMap<>();
    params.put("appid", appid);
    params.put("mch_id", mchId);
    params.put("out_trade_no", outTradeNo);
    params.put("nonce_str", genNonce());
    params.put("sign", sign(params, mchKey));

    String resp = postXml(CLOSE_ORDER_URL, mapToXml(params));
    Map<String, String> r = xmlToMap(resp);
    if (!"SUCCESS".equals(r.get("return_code"))) {
      log.warn("微信关单失败 outTradeNo={}: {}", outTradeNo, r);
      throw new BizException(1001, "微信关单失败：" + r.get("return_msg"));
    }
  }

  /** 退款：原路退回（需证书，见类注释）。 */
  public void refund(String outTradeNo, String transactionId, BigDecimal amountYuan) {
    ensureConfig();
    Map<String, String> params = new HashMap<>();
    params.put("appid", appid);
    params.put("mch_id", mchId);
    params.put("nonce_str", genNonce());
    params.put("out_trade_no", outTradeNo);
    params.put("out_refund_no", "RF" + System.currentTimeMillis());
    params.put("transaction_id", transactionId);
    params.put("total_fee", yuanToFen(amountYuan));
    params.put("refund_fee", yuanToFen(amountYuan));
    params.put("sign", sign(params, mchKey));

    String resp = postXml(REFUND_URL, mapToXml(params));
    Map<String, String> r = xmlToMap(resp);
    if (!"SUCCESS".equals(r.get("return_code")) || !"SUCCESS".equals(r.get("result_code"))) {
      log.warn("微信退款失败 outTradeNo={}: {}", outTradeNo, r);
      throw new BizException(1001, "微信退款失败：" + r.get("return_msg"));
    }
  }

  /** 解析并验签微信支付回调报文，返回字段 map（已剔除 sign）。 */
  public Map<String, String> parseNotify(String xml) {
    Map<String, String> map = xmlToMap(xml);
    if (!"SUCCESS".equals(map.get("return_code"))) {
      throw new BizException(1001, "微信回调 return_code 非 SUCCESS");
    }
    String sign = map.get("sign");
    String calc = sign(map, mchKey);
    if (sign == null || !sign.equals(calc)) {
      log.warn("微信回调签名校验失败");
      throw new BizException(1001, "微信回调签名校验失败");
    }
    return map;
  }

  // ---------------- 内部工具 ----------------

  /** 构造小程序支付参数（含 paySign）。 */
  private Map<String, String> buildPayParams(String prepayId) {
    Map<String, String> p = new HashMap<>();
    p.put("appId", appid);
    p.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
    p.put("nonceStr", genNonce());
    p.put("package", "prepay_id=" + prepayId);
    p.put("signType", "MD5");
    p.put("paySign", sign(p, mchKey));
    return p;
  }

  /** MD5 签名：按 key 字典序拼接 k=v&，末尾 &key=KEY，MD5 后转大写。 */
  String sign(Map<String, String> params, String key) {
    List<String> keys = new ArrayList<>(params.keySet());
    Collections.sort(keys);
    StringBuilder sb = new StringBuilder();
    for (String k : keys) {
      String v = params.get(k);
      if (v == null || v.isEmpty() || "sign".equals(k)) {
        continue;
      }
      sb.append(k).append("=").append(v).append("&");
    }
    sb.append("key=").append(key);
    return md5Hex(sb.toString()).toUpperCase();
  }

  private String yuanToFen(BigDecimal yuan) {
    return String.valueOf(yuan.multiply(BigDecimal.valueOf(100))
        .setScale(0, RoundingMode.HALF_UP).intValue());
  }

  private String genNonce() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
  }

  private String postXml(String url, String xml) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_XML);
    HttpEntity<String> entity = new HttpEntity<>(xml, headers);
    return restTemplate.postForObject(url, entity, String.class);
  }

  String mapToXml(Map<String, String> params) {
    StringBuilder sb = new StringBuilder("<xml>");
    for (Map.Entry<String, String> e : params.entrySet()) {
      sb.append("<").append(e.getKey()).append(">")
        .append("<![CDATA[").append(e.getValue() == null ? "" : e.getValue()).append("]]>")
        .append("</").append(e.getKey()).append(">");
    }
    return sb.append("</xml>").toString();
  }

  Map<String, String> xmlToMap(String xml) {
    Map<String, String> map = new HashMap<>();
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
      Element root = doc.getDocumentElement();
      NodeList list = root.getChildNodes();
      for (int i = 0; i < list.getLength(); i++) {
        Node n = list.item(i);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
          map.put(n.getNodeName(), n.getTextContent());
        }
      }
      return map;
    } catch (Exception e) {
      throw new BizException(1001, "解析微信报文失败");
    }
  }

  private String md5Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] bytes = md.digest(s.getBytes("UTF-8"));
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new BizException(500, "签名计算失败");
    }
  }

  private void ensureConfig() {
    if (appid == null || appid.isEmpty() || mchId == null || mchId.isEmpty()
        || mchKey == null || mchKey.isEmpty()) {
      log.error("服务端未配置微信支付参数（app.wx.appid / app.wx.pay.mch-id / app.wx.pay.mch-key）");
      throw new BizException(1001, "服务端未配置微信支付参数");
    }
  }
}
