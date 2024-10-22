package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     * 使用redis查询商铺信息
     * @param id
     * @return
     */
    Result queryById(Long id);

    /**
     * 使用redis，在修改店铺信息时删除redis
     * @param shop
     * @return
     */
    Result updateShopById(Shop shop);
}
