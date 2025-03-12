package com.hong.hotdealservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan(basePackages = "com.hong.common")
@EnableFeignClients
@EnableScheduling
public class AppConfig {
}
