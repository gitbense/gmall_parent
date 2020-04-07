package com.atguigu.gmall.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * @author zr
 * @create 2020-04-07 下午 16:42
 */
@Configuration
public class KeyResolverConfig {

    // 通过userKey 用户限流
//    @Bean
//    KeyResolver userKeyResolver() {
//        System.out.println("用户限流");
//        return exchange -> Mono.just(exchange.getRequest().getHeaders().get("token").get(0));
//    }

    // 通过Ip限流
//    @Bean
//    KeyResolver ipKeyResolver() {
//        System.out.println("Ip限流");
//        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
//    }

    // 通过api接口限流
    @Bean
    KeyResolver apiKeyResolver() {
        System.out.println("api接口限流");
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }
}
