package com.atguigu.gmall.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author zr
 * @create 2020-03-23 下午 20:41
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan({"com.atguigu.gmall"})
@EnableFeignClients(basePackages = {"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceListApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceListApplication.class, args);
    }
}
