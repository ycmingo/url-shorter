package com.scdt.urlshorter.interceptor;

import com.google.common.util.concurrent.RateLimiter;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * 限流拦截器
 *
 * @author mingo
 */
@Component
public class RateLimitInterceptor extends HandlerInterceptorAdapter
{
    public enum LimitType
    {
        /**
         * 丢弃
         */
        DROP,
        /**
         * 等待
         */
        WAIT
    }

    /**
     * Guava限流器
     */
    private RateLimiter rateLimiter;

    /**
     * 限流默认采用「丢弃」方式
     */
    private LimitType limitType = LimitType.DROP;

    /**
     * 默认构造
     */
    public RateLimitInterceptor()
    {
        this.rateLimiter = RateLimiter.create(1);
    }

    /**
     * 构造函数
     *
     * @param tps       每秒处理的量
     * @param limitType 限流类型
     */
    public RateLimitInterceptor(int tps, RateLimitInterceptor.LimitType limitType)
    {
        this.rateLimiter = RateLimiter.create(tps);
        this.limitType = limitType;
    }

    /**
     * 构造函数
     *
     * @param permitsPerSecond 每秒新增的令牌数
     * @param limitType        限流类型
     */
    public RateLimitInterceptor(double permitsPerSecond, RateLimitInterceptor.LimitType limitType)
    {
        this.rateLimiter = RateLimiter.create(permitsPerSecond, 1000, TimeUnit.MILLISECONDS);
        this.limitType = limitType;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (limitType.equals(LimitType.DROP))
        {
            if (rateLimiter.tryAcquire())
            {
                return super.preHandle(request, response, handler);
            }
        }
        throw new RuntimeException("服务器繁忙，请稍候再试");
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception
    {
        super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception
    {
        super.afterCompletion(request, response, handler, ex);
    }

    public RateLimiter getRateLimiter()
    {
        return rateLimiter;
    }

    public void setRateLimiter(RateLimiter rateLimiter)
    {
        this.rateLimiter = rateLimiter;
    }
}
