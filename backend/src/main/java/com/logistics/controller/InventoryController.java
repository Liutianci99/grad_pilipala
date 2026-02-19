package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.dto.InventoryQueryRequest;
import com.logistics.dto.StockInRequest;
import com.logistics.entity.Inventory;
import com.logistics.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 库存管理Controller
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "库存管理", description = "商品库存入库、查询等相关接口")
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    /**
     * 商品入库
     * @param request 入库请求数据
     * @param imageFile 商品图片文件
     * @return 入库结果
     */
    @Operation(summary = "商品入库", description = "商户将商品入库，包括商品信息和图片上传")
    @PostMapping("/stock-in")
    public Result<Inventory> stockIn(
            @Parameter(description = "入库请求数据") @Valid @ModelAttribute StockInRequest request,
            @Parameter(description = "商品图片文件") @RequestParam("image") MultipartFile imageFile) {
        
        Inventory inventory = inventoryService.stockIn(request, imageFile);
        return Result.success(inventory);
    }
    
    /**
     * 查询库存列表
     * @param request 查询条件
     * @return 库存列表
     */
    @Operation(summary = "查询库存列表", description = "查询商品库存列表，支持多种条件筛选")
    @GetMapping("/list")
    public Result<List<Inventory>> queryInventory(
            @Parameter(description = "查询条件") InventoryQueryRequest request) {
        List<Inventory> list = inventoryService.queryInventory(request);
        return Result.success(list);
    }
}
