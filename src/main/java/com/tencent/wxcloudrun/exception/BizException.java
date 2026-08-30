package com.tencent.wxcloudrun.exception;

/**
 * 业务异常：携带业务码 + 提示信息，由 GlobalExceptionHandler 统一转为 Result。
 */
public class BizException extends RuntimeException {

  private static final long serialVersionUID = 1L;

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