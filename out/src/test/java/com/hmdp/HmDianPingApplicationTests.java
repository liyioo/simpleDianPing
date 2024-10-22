package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    private CacheClient cacheClient;

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    public void RedisLogicExpireTest(){
//        shopService.saveShopToRedis(1L,20L);
        cacheClient.setWithLogicExpire(RedisConstants.CACHE_SHOP_KEY + "1",shopService.getById(1L),10L, TimeUnit.SECONDS);

    }

    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(300);
        Runnable task = ()->{
          for(int i = 0;i < 100;i++){
              long id = redisIdWorker.nextId("order");
              System.out.println("id = " + id);
          }
          countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for(int i = 0;i < 300;i++){
            es.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("end = " + end);
        System.out.println("time = " + (end - begin));

    }

    @Test
    public void test(){
        boolean f = false;
        Boolean g = new Boolean(false);
        Boolean b = new Boolean(false);
        System.out.println(g == b);
        Integer i = new Integer(0);
        Integer j = new Integer(0);
        System.out.println(i == j);
        Integer a = 20;

    }




}
