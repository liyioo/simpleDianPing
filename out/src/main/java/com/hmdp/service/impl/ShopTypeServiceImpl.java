package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryList() {
        //1.去缓存查询店铺类型
        String key = RedisConstants.CACHE_SHOPTYPE_KEY;
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
        //2.如果有结果，直接返回
        List<ShopType> shopTypeList;
        if(StrUtil.isNotBlank(shopTypeJson)){
            shopTypeList = JSONUtil.toList(JSONUtil.parseArray(shopTypeJson), ShopType.class);
            return shopTypeList;
        }

        //3.如果没有数据，那就到数据库里查询
        shopTypeList = getBaseMapper().selectList(null);
        //4.把数据库查询的结果传给redis
        if(!shopTypeList.isEmpty()){
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypeList));
        }

        //5.返回结果
        return shopTypeList;
    }
}
