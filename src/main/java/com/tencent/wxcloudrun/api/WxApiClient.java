package com.tencent.wxcloudrun.api;

import com.tencent.wxcloudrun.exception.BizException;

/**
 * 微信开放能力客户端。
 */
public interface WxApiClient {

  /**
   * jscode2session：用前端 wx.login() 拿到的 code 换取 openid。
   *
   * @param code 前端临时登录凭证 code
   * @return 用户 openid
   * @throws BizException 配置缺失 / 微信返回错误 / 拿不到 openid 时抛出（code=1001）
   */
  String code2Session(String code);

  /**
   * 用 getPhoneNumber 返回的 code 换取手机号。
   *
   * @param phoneCode 前端通过 open-type="getPhoneNumber" 拿到的 code
   * @return 用户手机号（纯数字，非脱敏）
   * @throws BizException 配置缺失 / 微信返回错误 / 拿不到手机号时抛出
   */
  String getPhoneNumber(String phoneCode);
}
