package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("token");
        // 1.获取请求头中的token令牌
        String token = request.getHeader("authorization");
        if(token == null ){
            return true;
        }
        String token_key = RedisConstants.LOGIN_USER_KEY + token;
        //2.获取redis的user
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(token_key);
//        UserDTO userDTO = (UserDTO) session.getAttribute("userDTO");
        //3.如果user不存在，返回false，拦截请求,返回401
        if(userMap.isEmpty() ){
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //4.如果user不为null，把user存入threadLocal
        UserHolder.saveUser(userDTO);

        //刷新token有效期
        stringRedisTemplate.expire(token_key,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //5.返回true
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清除用户信息
        UserHolder.removeUser();
    }
}
