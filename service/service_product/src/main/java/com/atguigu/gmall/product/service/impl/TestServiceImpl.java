package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zr
 * @create 2020-03-20 上午 11:08
 */
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public synchronized void testLock() {
        //自定义锁
        RLock lock = redissonClient.getLock("lock");
        //加锁，设置过期时间
        lock.lock(10l, TimeUnit.SECONDS);

        //查询redis中的num值
        String value = redisTemplate.opsForValue().get("num");
        //判断是否为空
        if (StringUtils.isBlank(value)) {
            //为空
            return;
        }
        //把value类型转成int
        int num = Integer.parseInt(value);
        //将redis中的值+1
        redisTemplate.opsForValue().set("num", String.valueOf(++num));

        //解锁，10s之后自动释放，也可以手动释放
        lock.unlock();
    }

    @Override
    public String readLock() {
        //初始化读写锁
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("readwriteLock");
        //读锁
        RLock rLock = rwLock.readLock();
        rLock.lock(10l, TimeUnit.SECONDS);
        //读取数据
        String msg = redisTemplate.opsForValue().get("msg");

        //解锁
        //rLock.unlock();
        return msg;
    }

    @Override
    public String writeLock() {
        //初始化读写锁
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("readwriteLock");
        //写锁
        RLock rLock = rwLock.writeLock();
        rLock.lock(10l, TimeUnit.SECONDS);
        //写入数据
        redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());

        //解锁
        //rLock.unlock();
        return "写入完成。。。。。。";
    }

    private void rTrylock(RLock lock) {
        try {
            //加锁，设置过期时间
            boolean res = lock.tryLock(100l, 10l, TimeUnit.SECONDS);
            if (res) {
                try {
                    //查询redis中的num值
                    String value = redisTemplate.opsForValue().get("num");
                    //判断是否为空
                    if (StringUtils.isBlank(value)) {
                        //为空
                        return;
                    }
                    //把value类型转成int
                    int num = Integer.parseInt(value);
                    //将redis中的值+1
                    redisTemplate.opsForValue().set("num", String.valueOf(++num));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //解锁
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testRedis() {
        //声明一个uuid
        String uuid = UUID.randomUUID().toString();
        //从redis中获取锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        if (lock) {
            //查询redis中的num值
            String value = redisTemplate.opsForValue().get("num");
            //判断是否为空
            if (StringUtils.isBlank(value)) {
                //为空
                return;
            }
            //把value类型转成int
            int num = Integer.parseInt(value);
            //将redis中的值+1
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //声明script--lua脚本
            /*
            if redis.call("get",KEYS[1]) == ARGV[1]
            then
                return redis.call("del",KEYS[1])
            else
                return 0
            end
             */
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            //设置lua脚本返回的数据类型
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            //设置lua脚本返回类型为Long类型
            redisScript.setResultType(Long.class);
            redisScript.setScriptText(script);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), uuid);
//            if (uuid.equals(redisTemplate.opsForValue().get("lock"))) {
//                //释放锁
//                redisTemplate.delete("lock");
//            }
        } else {
            //每隔一秒钟回调一次，再次尝试获取锁
            try {
                Thread.sleep(500);
                testRedis();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
