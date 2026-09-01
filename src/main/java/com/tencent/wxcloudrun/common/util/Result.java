package com.tencent.wxcloudrun.common.util;

/**
 * 统一响应体（对齐功能文档 §7：{code, message, data, timestamp}；code=0 成功）。
 */
public class Result<T> {

  private int code;
  private String message;
  private T data;
  private long timestamp;

  private Result(int code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
    this.timestamp = System.currentTimeMillis();
  }

  public static <T> Result<T> ok(T data) {
    return new Result<>(0, "ok", data);
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null);
  }

  public static <T> Result<T> error(String message) {
    return new Result<>(500, message, null);
  }

  public int getCode() { return code; }
  public void setCode(int code) { this.code = code; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public T getData() { return data; }
  public void setData(T data) { this.data = data; }
  public long getTimestamp() { return timestamp; }
  public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}