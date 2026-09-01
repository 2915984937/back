package com.tencent.wxcloudrun.store.dao;

import com.tencent.wxcloudrun.store.dto.StoreListRequest;
import com.tencent.wxcloudrun.store.model.Store;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StoreMapper {

  Store selectById(@Param("id") Long id);

  List<Store> selectList(StoreListRequest req);

  int insert(Store store);

  int updateById(Store store);

  int deleteById(@Param("id") Long id);
}
