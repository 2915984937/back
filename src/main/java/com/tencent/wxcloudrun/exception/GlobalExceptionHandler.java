package com.tencent.wxcloudrun.exception;

import com.tencent.wxcloudrun.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BizException.class)
  public Result<Void> handleBiz(BizException e) {
    return Result.error(e.getCode(), e.getMessage());
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
  public Result<Void> handleBadRequest(Exception e) {
    return Result.error(400, "请求参数错误");
  }

  @ExceptionHandler(Exception.class)
  public Result<Void> handle(Exception e) {
    log.error("uncaught exception", e);
    return Result.error(500, "服务开小差了，请稍后再试");
  }
}