package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author zr
 * 将这个类放入spring容器
 * 利用spring aop 特性 做一个环绕通知
 * @create 2020-03-21 下午 20:25
 */
@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    // 通过aop实现自动添加缓存的效果
    // 返回数据类型：不一定是skuInfo
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point) {
        // 声明一个对象Object
        Object result = null;

        // 获取到传递的参数
        Object[] args = point.getArgs();
        //获取方法上的签名
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 得到注解
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        // 获取前缀 sku
        String prefix = gmallCache.prefix();
        // 定义key sku:[skuId] sku:[18]
        String key = prefix + Arrays.asList(args).toString();
        // 先查询缓存（第一：必须传递key，第二：必须知道缓存中存储的数据类型）
        result = cacheHit(signature, key);
        if (result != null) {
            // 缓存中有数据
            return result;
        }
        // 缓存中无此数据，查询数据库，为防止缓存击穿，需加锁
        RLock lock = redissonClient.getLock(key + RedisConst.SKULOCK_SUFFIX);
        try {
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if (res) {
                // 拿到了分布式锁
                // 获取业务数据，得到带注解的方法体的执行结果
                // 表示调用了 getSkuInfoDB(skuId)
                result = point.proceed(point.getArgs());
                if (result == null) {
                    // 数据库中无此数据，new一个空对象放入缓存，防止缓存穿透
                    Object o = new Object();
                    redisTemplate.opsForValue().set(key,JSONObject.toJSONString(o),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                    return o;
                }
                // 数据库中有数据，直接放入缓存
                redisTemplate.opsForValue().set(key, JSONObject.toJSONString(result),RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                return result;
            } else {
                // 没有拿到锁
                Thread.sleep(1000);
                // 获取缓存数据
                return cacheHit(signature,key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            lock.unlock();
        }

        return result;
    }

    /**
     * @param signature 可以获取到方法的返回值
     * @param key       需要获取数据使用
     * @return
     */
    private Object cacheHit(MethodSignature signature, String key) {
        // 获取数据 redis的string数据类型 key：value都是字符串
        String cache = (String) redisTemplate.opsForValue().get(key);
        // 判断数据是否为空
        if (StringUtils.isNotBlank(cache)) {
            // 有数据，将数据进行转化，返回
            // 获取方法返回的数据类型
            Class returnType = signature.getReturnType();
            return JSONObject.parseObject(cache, returnType);
        }
        // 为空
        return null;
    }
}
