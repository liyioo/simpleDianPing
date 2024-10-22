package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.手机号不合法直接返回
            return Result.fail("手机号输入错误，请重新输入");
        }

        //3.手机号合法，生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4.把验证码保存到 Redis,并设置有效期 2 min
        String code_key = RedisConstants.LOGIN_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(code_key,code);
        stringRedisTemplate.expire(code_key,RedisConstants.LOGIN_CODE_TTL,TimeUnit.MINUTES);
//        session.setAttribute(phone,code);//用手机号作为key，防止其他用户使用验证码登录
        //5.发送验证码（先不实现发送功能）
        log.debug("发送验证码成功，验证码：{}",code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.判断用户输入的手机号是否合法
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.手机号不合法直接返回
            return Result.fail("手机号输入错误，请重新输入");
        }
        //2.判断验证码是否正确
        String code = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
//        Object code = session.getAttribute(loginForm.getPhone());
        //验证码错误，直接返回
        if(loginForm.getCode() == null || !loginForm.getCode().equals(code)){
            return Result.fail("验证码错误");
        }
        //3.根据手机号查询用户，
        User user = query().eq("phone", loginForm.getPhone()).one();

        //4.如果用户为null，则注册用户，加入数据库
        if(user == null){
            user = createUserWithPhone(loginForm.getPhone());
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        //5.保存用户信息到Redis
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(), CopyOptions.create()
                .setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));

        //生成token
        String token = UUID.randomUUID().toString(true);
        String token_key = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(token_key,userMap);
//        session.setAttribute("userDTO",userDTO);
        stringRedisTemplate.expire(token_key,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);//设置redis中用户信息的有效期 30min
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone){
        User user = User.builder()
                .nickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10))
                .phone(phone)
                .createTime(LocalDateTime.now())
                .build();
        save(user);
        return user;
    }
}
