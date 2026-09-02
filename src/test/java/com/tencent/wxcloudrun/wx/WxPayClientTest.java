package com.tencent.wxcloudrun.wx;

import com.tencent.wxcloudrun.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WxPayClientTest {

  private WxPayClient client() {
    WxPayClient c = new WxPayClient();
    c.mchKey = "test-key-1234567890";
    return c;
  }

  @Test
  void sign_同输入结果确定且为32位大写MD5() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("appid", "wx123");
    m.put("mch_id", "mch1");
    m.put("out_trade_no", "ORD1");

    String s1 = client().sign(m, "test-key-1234567890");
    String s2 = client().sign(m, "test-key-1234567890");

    assertEquals(s1, s2);
    assertEquals(32, s1.length());
    assertTrue(s1.matches("[0-9A-F]+"));
  }

  @Test
  void mapToXml与xmlToMap可互转() {
    WxPayClient c = client();
    Map<String, String> m = new LinkedHashMap<>();
    m.put("appid", "wx123");
    m.put("out_trade_no", "ORD1");
    m.put("total_fee", "2000");

    String xml = c.mapToXml(m);
    Map<String, String> back = c.xmlToMap(xml);

    assertEquals("wx123", back.get("appid"));
    assertEquals("ORD1", back.get("out_trade_no"));
    assertEquals("2000", back.get("total_fee"));
  }

  @Test
  void parseNotify_签名正确返回字段() {
    WxPayClient c = client();
    Map<String, String> m = new LinkedHashMap<>();
    m.put("return_code", "SUCCESS");
    m.put("out_trade_no", "ORD1");
    m.put("transaction_id", "TXN1");
    m.put("total_fee", "2000");
    m.put("sign", c.sign(m, c.mchKey));

    String xml = c.mapToXml(m);
    Map<String, String> parsed = c.parseNotify(xml);

    assertEquals("ORD1", parsed.get("out_trade_no"));
    assertEquals("TXN1", parsed.get("transaction_id"));
  }

  @Test
  void parseNotify_签名被篡改则抛异常() {
    WxPayClient c = client();
    Map<String, String> m = new LinkedHashMap<>();
    m.put("return_code", "SUCCESS");
    m.put("out_trade_no", "ORD1");
    m.put("sign", c.sign(m, c.mchKey));
    String xml = c.mapToXml(m);

    // 篡改后重新打包（不改签名）
    String tampered = xml.replace("ORD1", "ORD2");

    assertThrows(BizException.class, () -> c.parseNotify(tampered));
  }
}
