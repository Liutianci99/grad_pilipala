package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.dto.ProductListingRequest;
import com.logistics.entity.Inventory;
import com.logistics.entity.Mall;
import com.logistics.service.MallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商城管理Controller
 */
@RestController
@RequestMapping("/mall")
@RequiredArgsConstructor
@Tag(name = "商城管理", description = "商品上架、下架、商城浏览等相关接口")
public class MallController {
    
    private final MallService mallService;
    
    /**
     * 获取未上架的商品列表
     * @param userId 用户ID
     * @return 未上架商品列表
     */
    @Operation(summary = "获取未上架商品", description = "商户获取库存中还未上架的商品列表")
    @GetMapping("/offline-products")
    public Result<List<Inventory>> getOfflineProducts(@Parameter(description = "用户ID") @RequestParam Integer userId) {
        List<Inventory> products = mallService.getOfflineProducts(userId);
        return Result.success(products);
    }
    
    /**
     * 商品上架
     * @param request 上架请求数据
     * @return 上架结果
     */
    @Operation(summary = "商品上架", description = "商户将商品发布到商城，使其可供用户浏览和购买")
    @PostMapping("/publish")
    public Result<Mall> publishProduct(@Parameter(description = "商品上架信息") @Valid @RequestBody ProductListingRequest request) {
        Mall mall = mallService.publishProduct(request);
        return Result.success(mall);
    }
    
    /**
     * 获取商城商品列表
     * @return 商城商品列表
     */
    @Operation(summary = "获取商城商品", description = "获取商城中所有已上架的商品列表，供用户浏览")
    @GetMapping("/products")
    public Result<List<Mall>> getMallProducts() {
        List<Mall> products = mallService.getMallProducts();
        return Result.success(products);
    }
}
