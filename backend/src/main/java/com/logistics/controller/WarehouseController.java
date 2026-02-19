package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.entity.Warehouse;
import com.logistics.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仓库管理 Controller
 */
@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "仓库管理", description = "仓库信息查询相关接口")
public class WarehouseController {
    
    private final WarehouseService warehouseService;
    
    /**
     * 获取所有仓库列表
     */
    @Operation(summary = "获取仓库列表", description = "获取系统中所有仓库的基本信息")
    @GetMapping("/list")
    public Result<List<Warehouse>> list() {
        List<Warehouse> warehouses = warehouseService.listAll();
        return Result.success(warehouses);
    }

    /**
     * 根据ID获取仓库详情
     */
    @Operation(summary = "获取仓库详情", description = "根据仓库ID获取仓库的详细信息")
    @GetMapping("/{id}")
    public Result<Warehouse> getById(@Parameter(description = "仓库ID") @PathVariable Integer id) {
        Warehouse warehouse = warehouseService.getById(id);
        return Result.success(warehouse);
    }
}
