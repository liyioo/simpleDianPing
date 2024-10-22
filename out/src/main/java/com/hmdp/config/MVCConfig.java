package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MVCConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //这个拦截器后执行，去threadLocal查看user信息是否存在，存在放行，不存在拦截
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/user/login",
                        "/user/code",
                        "/blog/hot",
                        "/upload/**",
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/voucher-order/**"
                ).order(1);
        //这个拦截器先执行，去redis查找是否存在user信息，把user信息放入threadLocal，不管数据有还是没有直接放行
        registry.addInterceptor(refreshTokenInterceptor).addPathPatterns("/**").order(0);

    }
}
