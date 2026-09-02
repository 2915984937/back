package com.tencent.wxcloudrun.booking.dao;

import com.tencent.wxcloudrun.booking.model.Order;
import com.tencent.wxcloudrun.store.model.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

  int insert(Order order);

  Order selectById(@Param("id") Long id);

  Order selectByOrderNo(@Param("orderNo") String orderNo);

  /** 用户订单列表（最近 50 条）。 */
  List<Order> selectByUser(@Param("userId") Long userId);

  /** 行锁：微信回调里重读状态用，FOR UPDATE 串行化并发写。 */
  Order selectForUpdateByOrderNo(@Param("orderNo") String orderNo);

  /** 行锁：锁一定存在的 biz_seat 行，串行化该座位所有预约请求（比锁订单行可靠）。 */
  Seat selectSeatForUpdate(@Param("seatId") Long seatId);

  /** 重叠区间计数：排除已完成(3)/取消(4)/退款(5)，待支付(0)/待用(1)/使用中(2) 都算占用。0 = 无冲突。 */
  int existsOverlap(@Param("seatId") Long seatId,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

  int updateStatusPaid(@Param("id") Long id, @Param("payTime") LocalDateTime payTime);

  int updateStatusCancelled(@Param("id") Long id, @Param("reason") String reason);

  int updateStatusRefunded(@Param("id") Long id);

  int updateStatusInUse(@Param("id") Long id, @Param("actualStart") LocalDateTime actualStart);

  int updateStatusCompleted(@Param("id") Long id, @Param("actualEnd") LocalDateTime actualEnd);

  /** 超时扫描：status=0 且 expire_at 已过，按时间升序分批取。 */
  List<Order> selectExpiredUnpaid(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
