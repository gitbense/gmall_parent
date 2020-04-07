package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zr
 * @create 2020-03-22 上午 11:20
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {

        /**
         * 核心线程数
         * 最大线程数
         * 表示空闲线程的存活时间
         * 存活时间的单位
         * 用于缓存任务的阻塞队列
         * 省略：
         * ThreadFactory：指定创建线程的工厂
         * handler：表示当阻塞队列已满，且线程数达到最大线程数时，线程池拒绝添加新任务时采取的策略
         */
        return new ThreadPoolExecutor(50, 500, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
    }
}
