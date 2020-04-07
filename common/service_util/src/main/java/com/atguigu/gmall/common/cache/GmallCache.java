package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zr
 * @Target 指定该注解在什么位置使用 在方法上使用
 * @Retention 指定注解的生命周期 java class runtime
 * @create 2020-03-21 下午 20:19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GmallCache {

    /**
     * 自定义属性
     * 缓存key的前缀
     * @return
     */
    String prefix() default "cache";
}
