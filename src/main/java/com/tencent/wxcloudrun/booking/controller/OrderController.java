package com.tencent.wxcloudrun.booking.controller;

import com.tencent.wxcloudrun.booking.dto.OrderCreateRequest;
import com.tencent.wxcloudrun.booking.dto.OrderVO;
import com.tencent.wxcloudrun.booking.service.OrderService;
import com.tencent.wxcloudrun.common.auth.AuthInterceptor;
import com.tencent.wxcloudrun.common.util.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/order")
@Validated
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  private Long userId(HttpServletRequest request) {
    return (Long) request.getAttribute(AuthInterceptor.ATTR_USER_ID);
  }

  /** 创建预约订单（需登录）。 */
  @PostMapping
  public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest req, HttpServletRequest request) {
    return Result.ok(orderService.createOrder(userId(request), req));
  }

  /** 订单详情（需本人）。 */
  @GetMapping("/{id}")
  public Result<OrderVO> detail(@PathVariable Long id, HttpServletRequest request) {
    return Result.ok(orderService.detail(userId(request), id));
  }

  /** 我的订单列表。 */
  @GetMapping("/mine")
  public Result<List<OrderVO>> mine(HttpServletRequest request) {
    return Result.ok(orderService.listMine(userId(request)));
  }

  /** 用户主动取消（仅待支付）。 */
  @PostMapping("/{id}/cancel")
  public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
    orderService.cancelOrder(userId(request), id, "用户主动取消");
    return Result.ok(null);
  }

  /** 开门（设备触发，置使用中）。 */
  @PostMapping("/{id}/open")
  public Result<Void> open(@PathVariable Long id, HttpServletRequest request) {
    orderService.markInUse(userId(request), id);
    return Result.ok(null);
  }

  /** 完成（到点/离店）。 */
  @PostMapping("/{id}/complete")
  public Result<Void> complete(@PathVariable Long id, HttpServletRequest request) {
    orderService.markCompleted(userId(request), id);
    return Result.ok(null);
  }
}
