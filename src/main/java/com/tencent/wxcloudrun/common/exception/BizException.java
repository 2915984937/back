package com.tencent.wxcloudrun.common.exception;

/**
 * 业务异常：携带业务码 + 提示信息，由 GlobalExceptionHandler 统一转为 Result。
 *
 * 错误码区间：
 *   1xxx  用户 / 鉴权
 *     1001 微信登录失败（code2session 无 openid）
 *     1004 用户不存在
 *     1005 账号已注销
 *     1006 账号封禁（永久/临时）
 *     401  未登录 / token 无效
 *   4xxx  参数校验
 *     400  参数错误
 *   5xxx  服务端
 *     500 兜底
 */
public class BizException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** 微信登录 / jscode2session 相关 */
  public static final int CODE_WX_LOGIN_FAILED   = 1001;
  /** 用户不存在 */
  public static final int CODE_USER_NOT_FOUND   = 1004;
  /** 账号已注销 */
  public static final int CODE_USER_DELETED     = 1005;
  /** 账号封禁 */
  public static final int CODE_USER_BANNED      = 1006;

  private final int code;

  public BizException(int code, String message) {
    super(message);
    this.code = code;
  }

  public BizException(String message) {
    this(500, message);
  }

  public int getCode() {
    return code;
  }
}