package com.logistics.controller;

import com.logistics.common.Result;
import com.logistics.entity.Address;
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
@CrossOrigin
@Tag(name = "地址管理", description = "用户收货地址管理相关接口")
public class AddressController {
    
    private final AddressService addressService;
    
    /**
     * 获取当前用户的所有地址
     */
    @Operation(summary = "获取用户地址列表", description = "获取当前登录用户保存的所有收货地址")
    @GetMapping("/list")
    public Result<List<Address>> list(HttpServletRequest request) {
        System.out.println("========== 地址列表查询接口被调用 ==========");
        
        Long userId = (Long) request.getAttribute("userId");
        System.out.println("从请求中获取的 userId: " + userId);
        
        if (userId == null) {
            System.out.println("userId 为空，返回未登录错误");
            return Result.error("未登录");
        }
        
        List<Address> addresses = addressService.listByUserId(userId);
        System.out.println("查询到的地址数量: " + (addresses != null ? addresses.size() : 0));
        System.out.println("地址数据: " + addresses);
        
        return Result.success(addresses);
    }
    
    /**
     * 添加地址
     */
    @Operation(summary = "添加收货地址", description = "添加新的收货地址")
    @PostMapping
    public Result<Address> add(@Parameter(description = "地址信息") @RequestBody Address address, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        address.setUserId(userId);
        return addressService.add(address);
    }
    
    /**
     * 更新地址
     */
    @Operation(summary = "更新收货地址", description = "更新已保存的收货地址信息")
    @PutMapping("/{id}")
    public Result<Address> update(
            @Parameter(description = "地址ID") @PathVariable Long id, 
            @Parameter(description = "更新的地址信息") @RequestBody Address address, 
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        address.setId(id);
        address.setUserId(userId);
        return addressService.update(address);
    }
    
    /**
     * 删除地址
     */
    @Operation(summary = "删除收货地址", description = "删除指定的收货地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "地址ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        return addressService.delete(id, userId);
    }
    
    /**
     * 设置默认地址
     */
    @Operation(summary = "设置默认地址", description = "将指定地址设置为默认收货地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@Parameter(description = "地址ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        return addressService.setDefault(id, userId);
    }
}
