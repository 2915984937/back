package com.tencent.wxcloudrun.booking.task;

import com.tencent.wxcloudrun.booking.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时订单扫描：每分钟执行一次，把「待支付且 expire_at 已过」的订单关单并置取消。
 * 扫描间隔(1min) << 超时阈值(15min)，最多延迟 1 分钟，业务无感。
 */
@Component
public class OrderExpireTask {

  private static final Logger log = LoggerFactory.getLogger(OrderExpireTask.class);

  private final OrderService orderService;

  public OrderExpireTask(OrderService orderService) {
    this.orderService = orderService;
  }

  @Scheduled(cron = "0 * * * * ?")
  public void run() {
    try {
      orderService.expireScan();
    } catch (Exception e) {
      log.error("超时订单扫描异常", e);
    }
  }
}
