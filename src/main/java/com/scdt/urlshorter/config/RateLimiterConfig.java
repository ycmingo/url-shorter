package com.scdt.urlshorter.config;

import com.scdt.urlshorter.interceptor.RateLimitInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 限流器配置
 *
 * @author mingo
 */
@Component
public class RateLimiterConfig implements WebMvcConfigurer
{
    /**
     * 增加限流拦截器
     *
     * @param registry InterceptorRegistry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        //设定每秒接收100个请求量
        registry.addInterceptor(new RateLimitInterceptor(100, RateLimitInterceptor.LimitType.DROP))
                .addPathPatterns("/**");
    }
}
