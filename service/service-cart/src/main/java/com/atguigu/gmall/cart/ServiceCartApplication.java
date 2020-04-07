package com.atguigu.gmall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author zr
 * @create 2020-03-27 上午 8:49
 */
@SpringBootApplication
@ComponentScan({"com.atguigu.gmall"})
@EnableFeignClients(basePackages = {"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceCartApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceCartApplication.class, args);
    }
}
