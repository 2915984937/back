package com.tencent.wxcloudrun.store.service;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.common.util.Result;
import com.tencent.wxcloudrun.store.dao.SeatMapper;
import com.tencent.wxcloudrun.store.dao.StoreMapper;
import com.tencent.wxcloudrun.store.dto.StoreCreateRequest;
import com.tencent.wxcloudrun.store.dto.StoreListRequest;
import com.tencent.wxcloudrun.store.dto.StoreVO;
import com.tencent.wxcloudrun.store.model.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreService {

  private static final double EARTH_RADIUS_KM = 6371.0;

  private final StoreMapper storeMapper;
  private final SeatMapper seatMapper;

  public StoreService(StoreMapper storeMapper, SeatMapper seatMapper) {
    this.storeMapper = storeMapper;
    this.seatMapper = seatMapper;
  }

  /** C 端：列表（支持搜索 + 距离排序）。 */
  public List<StoreVO> list(StoreListRequest req) {
    if (req.getStatus() == null) {
      req.setStatus(0); // 默认只查营业中
    }
    List<Store> stores = storeMapper.selectList(req);
    List<StoreVO> vos = stores.stream().map(this::toVO).collect(Collectors.toList());

    if (req.getLatitude() != null && req.getLongitude() != null) {
      for (StoreVO vo : vos) {
        vo.setDistance(haversine(req.getLatitude().doubleValue(), req.getLongitude().doubleValue(),
                                 vo.getLatitude().doubleValue(), vo.getLongitude().doubleValue()));
      }
      vos.sort(Comparator.comparingDouble(StoreVO::getDistance));
    }
    return vos;
  }

  /** C 端：详情。 */
  public StoreVO detail(Long id) {
    Store store = storeMapper.selectById(id);
    if (store == null) {
      throw new BizException(1004, "门店不存在");
    }
    return toVO(store);
  }

  /** 管理后台：创建门店。 */
  @Transactional
  public StoreVO create(StoreCreateRequest req) {
    Store store = new Store();
    store.setName(req.getName().trim());
    store.setAddress(req.getAddress());
    store.setLongitude(req.getLongitude());
    store.setLatitude(req.getLatitude());
    store.setCoverImage(req.getCoverImage());
    store.setPhone(req.getPhone());
    store.setLayoutImage(req.getLayoutImage());
    store.setStatus(req.getStatus());
    storeMapper.insert(store);
    return toVO(store);
  }

  /** 管理后台：更新门店。 */
  @Transactional
  public StoreVO update(Long id, StoreCreateRequest req) {
    Store store = storeMapper.selectById(id);
    if (store == null) {
      throw new BizException(1004, "门店不存在");
    }
    store.setName(req.getName().trim());
    store.setAddress(req.getAddress());
    store.setLongitude(req.getLongitude());
    store.setLatitude(req.getLatitude());
    store.setCoverImage(req.getCoverImage());
    store.setPhone(req.getPhone());
    store.setLayoutImage(req.getLayoutImage());
    store.setStatus(req.getStatus());
    storeMapper.updateById(store);
    return toVO(store);
  }

  /** 管理后台：删除门店（连带座位）。 */
  @Transactional
  public void delete(Long id) {
    Store store = storeMapper.selectById(id);
    if (store == null) {
      throw new BizException(1004, "门店不存在");
    }
    seatMapper.deleteByStoreId(id);
    storeMapper.deleteById(id);
  }

  private StoreVO toVO(Store store) {
    StoreVO vo = new StoreVO();
    vo.setId(store.getId());
    vo.setName(store.getName());
    vo.setAddress(store.getAddress());
    vo.setLongitude(store.getLongitude());
    vo.setLatitude(store.getLatitude());
    vo.setCoverImage(store.getCoverImage());
    vo.setPhone(store.getPhone());
    vo.setLayoutImage(store.getLayoutImage());
    vo.setStatus(store.getStatus());
    vo.setCreateTime(store.getCreateTime());
    vo.setUpdateTime(store.getUpdateTime());
    return vo;
  }

  /** 应用层 haversine 距离（km），保留 1 位小数。 */
  public static double haversine(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double km = EARTH_RADIUS_KM * c;
    return BigDecimal.valueOf(km).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }
}
