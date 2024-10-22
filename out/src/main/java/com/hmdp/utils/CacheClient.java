package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class CacheClient {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long expireTime, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),expireTime,unit);
    }

    public void setWithLogicExpire(String key, Object value, Long expireTime, TimeUnit unit) {
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long expireTime, TimeUnit unit) {
        //1.根据商铺id去redis查询商铺信息
        String key = keyPrefix + id;
        String Json = stringRedisTemplate.opsForValue().get(key);


        //2.如果存在，直接返回
        if (StrUtil.isNotBlank(Json)) {
            return JSONUtil.toBean(Json, type);
        }
        //如果redis的店铺信息为"",返回错误信息，不要查询数据库,解决缓存穿透
        if (Json != null) {
            return null;
        }

        //3.如果不存在，去数据库里查询
        R r = dbFallback.apply(id);
        //4.如果数据库里也不存在，返回错误
        if (r == null) {
            //把空值存入redis，并设置有效期
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //5.如果数据库里存在，把数据加入到redis,并设置超时时间
       this.set(key,r,expireTime,unit);
        //6.返回数据
        return r;
    }

    //缓存重建的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R,ID> R queryWithLogicExpire(
            String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long expireTime, TimeUnit unit) {
        //1.根据商铺id去redis查询商铺信息
        String key = keyPrefix + id;
        String Json = stringRedisTemplate.opsForValue().get(key);

        //2.如果未命中，直接返回null
        if (StrUtil.isBlank(Json)) {
            return null;
        }
        //如果命中了，去查看是否过期,把JSON字符串反序列化为shop对象
        RedisData redisData = JSONUtil.toBean(Json, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            //如果未过期，直接返回结果
            return r;
        }
        //过期的话需要尝试获取锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        if (tryLock(lockKey)) {
            //如果获取成功，
            // 需要开启一个独立线程重建数据，写入redis，更新过期时间
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建redis，1.需要查询数据库 2.写入redis
                    R r1 = dbFallback.apply(id);
                    setWithLogicExpire(key,r1,expireTime,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //返回过期的结果
        return r;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private boolean unLock(String key) {
        Boolean flag = stringRedisTemplate.delete(key);
        return BooleanUtil.isTrue(flag);
    }


}
