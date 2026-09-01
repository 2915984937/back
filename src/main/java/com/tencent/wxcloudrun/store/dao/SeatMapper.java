package com.tencent.wxcloudrun.store.dao;

import com.tencent.wxcloudrun.store.model.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeatMapper {

  Seat selectById(@Param("id") Long id);

  List<Seat> selectByStoreId(@Param("storeId") Long storeId);

  int insert(Seat seat);

  int updateById(Seat seat);

  int deleteById(@Param("id") Long id);

  int deleteByStoreId(@Param("storeId") Long storeId);

  int countByStoreId(@Param("storeId") Long storeId);
}
