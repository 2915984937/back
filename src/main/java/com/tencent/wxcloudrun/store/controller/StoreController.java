package com.tencent.wxcloudrun.store.controller;

import com.tencent.wxcloudrun.common.util.Result;
import com.tencent.wxcloudrun.store.dto.StoreCreateRequest;
import com.tencent.wxcloudrun.store.dto.StoreListRequest;
import com.tencent.wxcloudrun.store.dto.StoreVO;
import com.tencent.wxcloudrun.store.service.SeatService;
import com.tencent.wxcloudrun.store.service.StoreService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/store")
public class StoreController {

  private final StoreService storeService;
  private final SeatService seatService;

  public StoreController(StoreService storeService, SeatService seatService) {
    this.storeService = storeService;
    this.seatService = seatService;
  }

  /** C 端：门店列表（公开）。 */
  @GetMapping("/list")
  public Result<List<StoreVO>> list(@Valid StoreListRequest req) {
    return Result.ok(storeService.list(req));
  }

  /** C 端：门店详情（公开）。 */
  @GetMapping("/{id}")
  public Result<StoreVO> detail(@PathVariable Long id) {
    return Result.ok(storeService.detail(id));
  }

  /** C 端：门店座位列表（公开）。 */
  @GetMapping("/{id}/seats")
  public Result<Object> seats(@PathVariable Long id) {
    return Result.ok(seatService.listByStore(id));
  }

  // ---- 管理后台（临时放在同一 Controller，可拆分） ----

  @PostMapping("/admin/store")
  public Result<StoreVO> adminCreate(@Valid @RequestBody StoreCreateRequest req) {
    return Result.ok(storeService.create(req));
  }

  @PutMapping("/admin/store/{id}")
  public Result<StoreVO> adminUpdate(@PathVariable Long id,
                                     @Valid @RequestBody StoreCreateRequest req) {
    return Result.ok(storeService.update(id, req));
  }

  @DeleteMapping("/admin/store/{id}")
  public Result<Void> adminDelete(@PathVariable Long id) {
    storeService.delete(id);
    return Result.ok(null);
  }
}
