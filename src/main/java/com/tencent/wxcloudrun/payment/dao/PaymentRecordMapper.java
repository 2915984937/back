package com.tencent.wxcloudrun.payment.dao;

import com.tencent.wxcloudrun.payment.model.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRecordMapper {

  int insert(PaymentRecord record);

  /** 幂等前置检查：同一微信交易号是否已落库。 */
  int countByTransactionId(@Param("txn") String txn);
}
