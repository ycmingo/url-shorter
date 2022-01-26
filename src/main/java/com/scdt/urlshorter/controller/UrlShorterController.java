package com.scdt.urlshorter.controller;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.scdt.urlshorter.annotation.RateLimit;
import com.scdt.urlshorter.dto.GenerateUrlRequest;
import com.scdt.urlshorter.dto.ResultResponse;
import com.scdt.urlshorter.utils.ConversionUtil;
import com.scdt.urlshorter.utils.SnowFlake;
import com.scdt.urlshorter.utils.cache.LruCacheUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mingo
 */
@Api(value = "短域名API")
@RestController
public class UrlShorterController
{
    /**
     * 短——长域名映射表
     */
    private final LruCacheUtil<String, String> shortUrlCache = new LruCacheUtil<>(1000);

    /**
     * 长——短域名映射表
     */
    private final LruCacheUtil<String, String> originalUrlCache = new LruCacheUtil<>(1000);

    private final BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 1000, 0.0001);

    @RateLimit(key = "UrlShorterController#generate", permitsPerSecond = 1000, timeout = 500)
    @ApiOperation(value = "生成短域名地址")
    @PostMapping("/generate")
    public ResultResponse generate(@RequestBody GenerateUrlRequest request)
    {
        if (request == null || StringUtils.isEmpty(request.getUrl()))
        {
            return ResultResponse.error("请输入正确的URL");
        }
        String shortUrl;

        //判断缓存中是否置换过该原始URL
        shortUrl = originalUrlCache.get(request.getUrl());
        if (StringUtils.isNotBlank(shortUrl) && StringUtils.isNotBlank(shortUrlCache.get(shortUrl)))
        {
            //取出之前的短URL
            return ResultResponse.success().put("code", shortUrlCache.get(shortUrl));
        }

        shortUrl = generateShorterUrl();
        boolean exist = bloomFilter.mightContain(shortUrl);
        if (!exist)
        {
            bloomFilter.put(shortUrl);
            shortUrlCache.put(shortUrl, request.getUrl());
            originalUrlCache.put(request.getUrl(), shortUrl);
        }
        else
        {
            String originalUrl = shortUrlCache.get(shortUrl);
            if (StringUtils.isNotBlank(originalUrl) && request.getUrl().equalsIgnoreCase(originalUrl))
            {
                shortUrl = generateShorterUrl();
                bloomFilter.put(shortUrl);
                shortUrlCache.put(shortUrl, request.getUrl());
                originalUrlCache.put(request.getUrl(), shortUrl);
            }
        }
        return ResultResponse.success().put("code", shortUrl);
    }

    /**
     * 生成短URL
     *
     * @return 短URL
     */
    private String generateShorterUrl()
    {
        long urlId = SnowFlake.nextId();
        HashFunction function = Hashing.murmur3_32();
        HashCode hashCode = function.hashString(String.valueOf(urlId), StandardCharsets.UTF_8);
        return ConversionUtil.encode(hashCode.asInt(), 5);
    }

    @ApiOperation(value = "通过短域名获取实际URL")
    @GetMapping("/query/{code}")
    public ResultResponse query(@PathVariable(name = "code") String code)
    {
        if (StringUtils.isEmpty(code))
        {
            return ResultResponse.error("参数不能为空");
        }
        //从缓存中取得原始URL
        String originalUrl = shortUrlCache.get(code);
        if (StringUtils.isEmpty(originalUrl))
        {
            //缓存中没有则表示数据已过期
            return ResultResponse.error("短链接地址已过期");
        }
        //返回实际的URL
        return ResultResponse.success().put("url", originalUrl);
    }
}
