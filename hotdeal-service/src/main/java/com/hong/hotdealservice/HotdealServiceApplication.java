package com.hong.hotdealservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.hong.hotdealservice", "com.hong.common"})
@EnableFeignClients
public class HotdealServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotdealServiceApplication.class, args);
    }

}
