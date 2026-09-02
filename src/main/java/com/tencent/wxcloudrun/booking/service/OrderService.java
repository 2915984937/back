package com.tencent.wxcloudrun.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.booking.dao.OrderMapper;
import com.tencent.wxcloudrun.booking.dto.OrderCreateRequest;
import com.tencent.wxcloudrun.booking.dto.OrderVO;
import com.tencent.wxcloudrun.booking.model.Order;
import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.mqtt.MqttPublisher;
import com.tencent.wxcloudrun.payment.dao.PaymentRecordMapper;
import com.tencent.wxcloudrun.payment.model.PaymentRecord;
import com.tencent.wxcloudrun.store.dao.SeatMapper;
import com.tencent.wxcloudrun.store.dao.StoreMapper;
import com.tencent.wxcloudrun.store.model.Seat;
import com.tencent.wxcloudrun.store.model.Store;
import com.tencent.wxcloudrun.wx.WxPayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 预约订单核心服务。
 *
 * 设计对齐概要设计.md：无 Redis，防双订靠「锁座位行 + 重叠查询」，超时靠 expire_at 扫描。
 */
@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  /** 待支付超时时长（分钟）。 */
  private static final int PAY_TIMEOUT_MINUTES = 15;

  private final OrderMapper orderMapper;
  private final SeatMapper seatMapper;
  private final StoreMapper storeMapper;
  private final PaymentRecordMapper paymentRecordMapper;
  private final WxPayClient wxPayClient;
  private final MqttPublisher mqttPublisher;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public OrderService(OrderMapper orderMapper, SeatMapper seatMapper, StoreMapper storeMapper,
                      PaymentRecordMapper paymentRecordMapper, WxPayClient wxPayClient,
                      MqttPublisher mqttPublisher) {
    this.orderMapper = orderMapper;
    this.seatMapper = seatMapper;
    this.storeMapper = storeMapper;
    this.paymentRecordMapper = paymentRecordMapper;
    this.wxPayClient = wxPayClient;
    this.mqttPublisher = mqttPublisher;
  }

  /**
   * 创建订单：锁 biz_seat 行 + 查 biz_order 自身重叠区间，串行化该座位的并发预约。
   */
  @Transactional
  public OrderVO createOrder(Long userId, OrderCreateRequest req) {
    Seat seat = seatMapper.selectById(req.getSeatId());
    if (seat == null || !seat.isAvailable()) {
      throw new BizException(400, "座位不存在或暂不可预约");
    }
    Store store = storeMapper.selectById(req.getStoreId());
    if (store == null) {
      throw new BizException(400, "门店不存在");
    }
    LocalDateTime start = req.getBookingStart();
    LocalDateTime end = req.getBookingEnd();
    if (!end.isAfter(start)) {
      throw new BizException(400, "结束时间需晚于开始时间");
    }
    if (start.isBefore(LocalDateTime.now())) {
      throw new BizException(400, "不能预约过去的时间");
    }

    // 锁座位行（一定存在）→ 该座位所有预约请求被串行化
    orderMapper.selectSeatForUpdate(req.getSeatId());
    int overlap = orderMapper.existsOverlap(req.getSeatId(), start, end);
    if (overlap > 0) {
      throw new BizException(400, "该时段已被预约，请换个时间");
    }

    long minutes = Duration.between(start, end).toMinutes();
    BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    BigDecimal amount = seat.getPriceHour().multiply(hours).setScale(2, RoundingMode.HALF_UP);

    Order order = new Order();
    order.setOrderNo(genOrderNo());
    order.setUserId(userId);
    order.setStoreId(store.getId());
    order.setSeatId(seat.getId());
    order.setBookingStart(start);
    order.setBookingEnd(end);
    order.setDurationMin((int) minutes);
    order.setPayAmount(amount);
    order.setOrderStatus(0);
    order.setExpireAt(LocalDateTime.now().plusMinutes(PAY_TIMEOUT_MINUTES));
    orderMapper.insert(order);
    return toVO(order, seat.getSeatName());
  }

  /** 用户主动取消（仅待支付可取消）：先关微信订单，再置取消态。 */
  @Transactional
  public void cancelOrder(Long userId, Long orderId, String reason) {
    Order order = orderMapper.selectById(orderId);
    if (order == null) {
      throw new BizException(1004, "订单不存在");
    }
    if (!order.getUserId().equals(userId)) {
      throw new BizException(401, "无权操作该订单");
    }
    if (order.getOrderStatus() != 0) {
      throw new BizException(400, "订单已不可取消");
    }
    try {
      wxPayClient.closeOrder(order.getOrderNo());
    } catch (Exception e) {
      // 未支付时关单可能返回"订单不存在"，忽略
      log.warn("关单异常（可忽略） orderNo={}: {}", order.getOrderNo(), e.getMessage());
    }
    orderMapper.updateStatusCancelled(orderId, reason == null ? "用户主动取消" : reason);
  }

  /**
   * 微信回调落库：行锁重读状态 + 状态再审。
   *   - 仍是待支付 → 记支付记录、置待用(1)
   *   - 已取消却收款 → 原路退款、置退款(5)
   *   - 其它（已处理/使用中/完成）→ 直接 ack，避免重复入账
   */
  @Transactional
  public void confirmPaid(String orderNo, String transactionId, BigDecimal paidAmount) {
    Order order = orderMapper.selectForUpdateByOrderNo(orderNo);
    if (order == null) {
      return; // 订单不存在，回成功
    }
    if (order.getOrderStatus() != 0) {
      if (order.isCancelled() && paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
        doRefund(order, transactionId, paidAmount);
      }
      return;
    }
    insertPaymentRecord(order, transactionId, paidAmount, PaymentRecord.STATUS_SUCCESS);
    orderMapper.updateStatusPaid(order.getId(), LocalDateTime.now());
  }

  /** 定时任务：扫描超时未支付订单，关单并置取消。 */
  @Transactional
  public void expireScan() {
    List<Order> expired = orderMapper.selectExpiredUnpaid(LocalDateTime.now(), 500);
    for (Order order : expired) {
      try {
        wxPayClient.closeOrder(order.getOrderNo());
      } catch (Exception e) {
        log.warn("超时关单异常 orderNo={}: {}", order.getOrderNo(), e.getMessage());
      }
      orderMapper.updateStatusCancelled(order.getId(), "超时未支付");
    }
  }

  /** 设备开门后：置使用中(2)，并向 IoT 设备下发开锁指令。 */
  @Transactional
  public void markInUse(Long userId, Long orderId) {
    Order order = requireOwnedOrder(userId, orderId);
    if (order.getOrderStatus() != 1) {
      throw new BizException(400, "订单状态异常，无法开门");
    }
    openDoor(order.getSeatId());
    orderMapper.updateStatusInUse(order.getId(), LocalDateTime.now());
  }

  /** 到点/离店：置完成(3)。 */
  @Transactional
  public void markCompleted(Long userId, Long orderId) {
    Order order = requireOwnedOrder(userId, orderId);
    if (order.getOrderStatus() != 2) {
      throw new BizException(400, "订单尚未开始使用");
    }
    orderMapper.updateStatusCompleted(order.getId(), LocalDateTime.now());
  }

  /** 用户订单列表（自身）。 */
  public List<OrderVO> listMine(Long userId) {
    // 简化：直接按 user_id 查最近 50 条（实际可加状态/分页，此处给最小可用实现）
    List<Order> orders = orderMapper.selectByUser(userId);
    return orders.stream()
        .map(o -> toVO(o, seatName(o.getSeatId())))
        .collect(Collectors.toList());
  }

  public OrderVO detail(Long userId, Long orderId) {
    Order order = requireOwnedOrder(userId, orderId);
    return toVO(order, seatName(order.getSeatId()));
  }

  // ---------------- 内部辅助 ----------------

  public Order requireOwnedOrder(Long userId, Long orderId) {
    Order order = orderMapper.selectById(orderId);
    if (order == null) {
      throw new BizException(1004, "订单不存在");
    }
    if (!order.getUserId().equals(userId)) {
      throw new BizException(401, "无权操作该订单");
    }
    return order;
  }

  private String seatName(Long seatId) {
    Seat seat = seatMapper.selectById(seatId);
    return seat == null ? null : seat.getSeatName();
  }

  private void openDoor(Long seatId) {
    Seat seat = seatMapper.selectById(seatId);
    if (seat == null || seat.getDeviceId() == null || seat.getDeviceId().isEmpty()) {
      log.warn("座位无设备，跳过开门 seatId={}", seatId);
      return;
    }
    try {
      String payload = objectMapper.writeValueAsString(
          java.util.Collections.singletonMap("action", "open"));
      mqttPublisher.publish("iot/" + seat.getDeviceId() + "/cmd", payload);
    } catch (Exception e) {
      log.warn("开门指令下发失败 seatId={}: {}", seatId, e.getMessage());
    }
  }

  private void doRefund(Order order, String transactionId, BigDecimal amount) {
    try {
      wxPayClient.refund(order.getOrderNo(), transactionId, amount);
    } catch (Exception e) {
      log.error("原路退款失败 orderNo={}", order.getOrderNo(), e);
    }
    insertPaymentRecord(order, transactionId, amount, PaymentRecord.STATUS_REFUNDED);
    orderMapper.updateStatusRefunded(order.getId());
  }

  private void insertPaymentRecord(Order order, String transactionId, BigDecimal amount, int status) {
    try {
      PaymentRecord pr = new PaymentRecord();
      pr.setOrderId(order.getId());
      pr.setOrderNo(order.getOrderNo());
      pr.setPayNo("PAY" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999));
      pr.setTransactionId(transactionId);
      pr.setAmount(amount);
      pr.setPayStatus(status);
      paymentRecordMapper.insert(pr);
    } catch (DuplicateKeyException e) {
      // 重复回调：交易号唯一约束拦截，视为已处理
      log.info("支付记录已存在，忽略重复回调 orderNo={}", order.getOrderNo());
    }
  }

  private OrderVO toVO(Order order, String seatName) {
    OrderVO vo = new OrderVO();
    vo.setId(order.getId());
    vo.setOrderNo(order.getOrderNo());
    vo.setUserId(order.getUserId());
    vo.setStoreId(order.getStoreId());
    vo.setSeatId(order.getSeatId());
    vo.setSeatName(seatName);
    vo.setBookingStart(order.getBookingStart());
    vo.setBookingEnd(order.getBookingEnd());
    vo.setDurationMin(order.getDurationMin());
    vo.setPayAmount(order.getPayAmount());
    vo.setOrderStatus(order.getOrderStatus());
    vo.setCancelReason(order.getCancelReason());
    vo.setExpireAt(order.getExpireAt());
    vo.setPayTime(order.getPayTime());
    vo.setActualStart(order.getActualStart());
    vo.setActualEnd(order.getActualEnd());
    vo.setCreateTime(order.getCreateTime());
    return vo;
  }

  private String genOrderNo() {
    return "ORD" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        + ThreadLocalRandom.current().nextInt(1000, 9999);
  }
}
