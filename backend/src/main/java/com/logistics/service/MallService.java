package com.logistics.service;

import com.logistics.dto.ProductListingRequest;
import com.logistics.entity.Inventory;
import com.logistics.entity.Mall;

import java.util.List;

/**
 * 商城服务接口
 */
public interface MallService {
    
    /**
     * 获取未上架的商品列表
     * @param userId 用户ID
     * @return 未上架商品列表
     */
    List<Inventory> getOfflineProducts(Integer userId);
    
    /**
     * 商品上架
     * @param request 上架请求数据
     * @return 上架的商城商品
     */
    Mall publishProduct(ProductListingRequest request);
    
    /**
     * 获取商城商品列表
     * @return 商城商品列表
     */
    List<Mall> getMallProducts();
}
