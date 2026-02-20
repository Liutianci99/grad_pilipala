package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.entity.Address;
import com.logistics.exception.BusinessException;
import com.logistics.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 地址管理 Controller
 */
@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
@Tag(name = "地址管理", description = "用户收货地址管理相关接口")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "获取用户地址列表")
    @GetMapping("/list")
    public Result<List<Address>> listAddresses(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Address> addresses = addressService.listByUserId(userId);
        return Result.success(addresses);
    }

    @Operation(summary = "添加收货地址")
    @PostMapping
    public Result<Address> createAddress(@Parameter(description = "地址信息") @RequestBody Address address, HttpServletRequest request) {
        Long userId = getUserId(request);
        address.setUserId(userId);
        return addressService.add(address);
    }

    @Operation(summary = "更新收货地址")
    @PutMapping("/{id}")
    public Result<Address> updateAddress(
            @Parameter(description = "地址ID") @PathVariable Long id,
            @Parameter(description = "更新的地址信息") @RequestBody Address address,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        address.setId(id);
        address.setUserId(userId);
        return addressService.update(address);
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@Parameter(description = "地址ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        return addressService.delete(id, userId);
    }

    @Operation(summary = "设置默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefaultAddress(@Parameter(description = "地址ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        return addressService.setDefault(id, userId);
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
