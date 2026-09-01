package com.tencent.wxcloudrun.store.controller;

import com.tencent.wxcloudrun.common.util.Result;
import com.tencent.wxcloudrun.store.dto.SeatCreateRequest;
import com.tencent.wxcloudrun.store.dto.SeatVO;
import com.tencent.wxcloudrun.store.service.SeatService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/store/admin/seat")
public class SeatController {

  private final SeatService seatService;

  public SeatController(SeatService seatService) {
    this.seatService = seatService;
  }

  @PostMapping
  public Result<SeatVO> create(@Valid @RequestBody SeatCreateRequest req) {
    return Result.ok(seatService.create(req));
  }

  @PutMapping("/{id}")
  public Result<SeatVO> update(@PathVariable Long id,
                               @Valid @RequestBody SeatCreateRequest req) {
    return Result.ok(seatService.update(id, req));
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    seatService.delete(id);
    return Result.ok(null);
  }
}
