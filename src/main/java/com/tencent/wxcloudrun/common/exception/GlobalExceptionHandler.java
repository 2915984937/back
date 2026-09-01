package com.tencent.wxcloudrun.common.exception;

import com.tencent.wxcloudrun.common.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** 业务异常（4xx 居多：未登录、参数错、用户不存在...） */
  @ExceptionHandler(BizException.class)
  public Result<Void> handleBiz(BizException e) {
    return Result.error(e.getCode(), e.getMessage());
  }

  /** @Valid 校验失败：Controller 方法参数上的 DTO 字段校验 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Result<Void> handleValidation(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("；"));
    return Result.error(400, msg);
  }

  /** @Validated 校验失败：Controller 方法参数上的 @NotBlank 等 */
  @ExceptionHandler(ConstraintViolationException.class)
  public Result<Void> handleConstraint(ConstraintViolationException e) {
    String msg = e.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining("；"));
    return Result.error(400, msg);
  }

  /** 请求体 JSON 解析失败 / 缺必填参数 */
  @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
  public Result<Void> handleBadRequest(Exception e) {
    return Result.error(400, "请求参数错误");
  }

  /** 兜底：500 服务器内部错误 */
  @ExceptionHandler(Exception.class)
  public Result<Void> handle(Exception e) {
    log.error("uncaught exception", e);
    return Result.error(500, "服务开小差了，请稍后再试");
  }
}
