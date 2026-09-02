package com.tencent.wxcloudrun.payment.controller;

import com.tencent.wxcloudrun.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

/**
 * 微信支付回调（公开，无鉴权）。
 * 路径已在 WebMvcConfig 加入 AuthInterceptor 白名单。
 */
@RestController
@RequestMapping("/api/payment")
public class WxPayCallbackController {

  private final PaymentService paymentService;

  public WxPayCallbackController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/wx/notify")
  public String wxNotify(@RequestBody String xml) {
    return paymentService.handleNotify(xml);
  }
}
