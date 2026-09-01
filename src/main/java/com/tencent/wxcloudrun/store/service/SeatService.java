package com.tencent.wxcloudrun.store.service;

import com.tencent.wxcloudrun.common.exception.BizException;
import com.tencent.wxcloudrun.store.dao.SeatMapper;
import com.tencent.wxcloudrun.store.dao.StoreMapper;
import com.tencent.wxcloudrun.store.dto.SeatCreateRequest;
import com.tencent.wxcloudrun.store.dto.SeatVO;
import com.tencent.wxcloudrun.store.model.Seat;
import com.tencent.wxcloudrun.store.model.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatService {

  private final SeatMapper seatMapper;
  private final StoreMapper storeMapper;

  public SeatService(SeatMapper seatMapper, StoreMapper storeMapper) {
    this.seatMapper = seatMapper;
    this.storeMapper = storeMapper;
  }

  /** C 端 / 管理后台：查询某门店下全部座位。 */
  public List<SeatVO> listByStore(Long storeId) {
    Store store = storeMapper.selectById(storeId);
    if (store == null) {
      throw new BizException(1004, "门店不存在");
    }
    return seatMapper.selectByStoreId(storeId).stream()
        .map(this::toVO)
        .collect(Collectors.toList());
  }

  /** 管理后台：新增座位。 */
  @Transactional
  public SeatVO create(SeatCreateRequest req) {
    Store store = storeMapper.selectById(req.getStoreId());
    if (store == null) {
      throw new BizException(1004, "门店不存在");
    }
    Seat seat = new Seat();
    seat.setStoreId(req.getStoreId());
    seat.setSeatName(req.getSeatName().trim());
    seat.setPosX(req.getPosX());
    seat.setPosY(req.getPosY());
    seat.setPriceHour(req.getPriceHour());
    seat.setStatus(req.getStatus());
    seat.setDeviceId(req.getDeviceId());
    seatMapper.insert(seat);
    return toVO(seat);
  }

  /** 管理后台：修改座位。 */
  @Transactional
  public SeatVO update(Long id, SeatCreateRequest req) {
    Seat seat = seatMapper.selectById(id);
    if (seat == null) {
      throw new BizException(1004, "座位不存在");
    }
    seat.setStoreId(req.getStoreId());
    seat.setSeatName(req.getSeatName().trim());
    seat.setPosX(req.getPosX());
    seat.setPosY(req.getPosY());
    seat.setPriceHour(req.getPriceHour());
    seat.setStatus(req.getStatus());
    seat.setDeviceId(req.getDeviceId());
    seatMapper.updateById(seat);
    return toVO(seat);
  }

  /** 管理后台：删除座位。 */
  @Transactional
  public void delete(Long id) {
    Seat seat = seatMapper.selectById(id);
    if (seat == null) {
      throw new BizException(1004, "座位不存在");
    }
    seatMapper.deleteById(id);
  }

  private SeatVO toVO(Seat seat) {
    SeatVO vo = new SeatVO();
    vo.setId(seat.getId());
    vo.setStoreId(seat.getStoreId());
    vo.setSeatName(seat.getSeatName());
    vo.setPosX(seat.getPosX());
    vo.setPosY(seat.getPosY());
    vo.setPriceHour(seat.getPriceHour());
    vo.setStatus(seat.getStatus());
    vo.setDeviceId(seat.getDeviceId());
    vo.setCreateTime(seat.getCreateTime());
    vo.setUpdateTime(seat.getUpdateTime());
    return vo;
  }
}
