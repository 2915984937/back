package com.tencent.wxcloudrun.payment.controller;

import com.tencent.wxcloudrun.common.auth.AuthInterceptor;
import com.tencent.wxcloudrun.common.util.Result;
import com.tencent.wxcloudrun.payment.dto.PrepayRequest;
import com.tencent.wxcloudrun.payment.service.PaymentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Validated
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  /** 预下单：返回小程序 wx.requestPayment 所需参数。 */
  @PostMapping("/prepay")
  public Result<Map<String, String>> prepay(@Valid @RequestBody PrepayRequest req,
                                            HttpServletRequest request) {
    Long userId = (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
    return Result.ok(paymentService.createPrepay(userId, req.getOrderId()));
  }
}
