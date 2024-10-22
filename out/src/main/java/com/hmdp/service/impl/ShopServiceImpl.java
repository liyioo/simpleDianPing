package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = cacheClient
//                .queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY,id,Shop.class,
//                        id2 -> getById(id2),RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        //使用逻辑过期时间解决缓存击穿
        Shop shop = cacheClient.queryWithLogicExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);
        if (shop == null) {
            return Result.fail("店铺信息不存在");
        }
        return Result.ok(shop);
    }

//    //缓存重建的线程池
//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
//
//    public Shop queryWithLogicExpire(Long id) {
//        //1.根据商铺id去redis查询商铺信息
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//        //2.如果未命中，直接返回null
//        if (StrUtil.isBlank(shopJson)) {
//            return null;
//        }
//        //如果命中了，去查看是否过期,把JSON字符串反序列化为shop对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
//            //如果未过期，直接返回结果
//            return shop;
//        }
//        //过期的话需要尝试获取锁
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        if (tryLock(lockKey)) {
//            //如果获取成功，
//            // 需要开启一个独立线程重建数据，写入redis，更新过期时间
//
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    this.saveShopToRedis(id, RedisConstants.LOCK_SHOP_TTL);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unLock(lockKey);
//                }
//            });
//
//
//        }
//        //返回过期的结果
//        return shop;
//    }
//
//    public Shop queryWithMutex(Long id) {
//        //1.根据商铺id去redis查询商铺信息
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//        //2.如果存在，直接返回
//        if (StrUtil.isNotBlank(shopJson)) {
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //如果redis的店铺信息为"",返回错误信息，不要查询数据库,解决缓存穿透
//        if (shopJson != null) {
//            return null;
//        }
//        //实现缓存重建
//        String lock_key = RedisConstants.LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            if (!tryLock(lock_key)) {
//                //获取锁失败，休眠并重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //3.如果不存在，去数据库里查询
//            shop = getById(id);
//            //模拟重建的延时
//            Thread.sleep(200);
//            //4.如果数据库里也不存在，返回错误
//            if (shop == null) {
//                //把空值存入redis，并设置有效期
//                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            //5.如果数据库里存在，把数据加入到redis,并设置超时时间
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //释放锁
//            unLock(lock_key);
//        }
//
//        //6.返回数据
//        return shop;
//    }

//    public Shop queryWithPassThrough(Long id) {
//        //1.根据商铺id去redis查询商铺信息
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//
//        //2.如果存在，直接返回
//        if (StrUtil.isNotBlank(shopJson)) {
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //如果redis的店铺信息为"",返回错误信息，不要查询数据库,解决缓存穿透
//        if (shopJson != null) {
//            return null;
//        }
//
//        //3.如果不存在，去数据库里查询
//        Shop shop = getById(id);
//        //4.如果数据库里也不存在，返回错误
//        if (shop == null) {
//            //把空值存入redis，并设置有效期
//            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //5.如果数据库里存在，把数据加入到redis,并设置超时时间
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        //6.返回数据
//        return shop;
//    }
//

//    public void saveShopToRedis(Long id, Long expireSeconds) {
//        //查询店铺数据
//        Shop shop = getById(id);
//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        //封装过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        //写入redis,使用逻辑过期时间
//        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//    }

//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//
//    private boolean unLock(String key) {
//        Boolean flag = stringRedisTemplate.delete(key);
//        return BooleanUtil.isTrue(flag);
//    }


    @Override
    @Transactional
    public Result updateShopById(Shop shop) {
        //判断id是否为null
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为null");
        }
        //先更新数据库
        updateById(shop);
        //再删除缓存,对于在单系统同时进行数据库和redis的删改操作，要加上事务
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
