package com.hong.hotdealservice;

import com.hong.hotdealservice.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(AppConfig.class)
public class HotdealServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotdealServiceApplication.class, args);
    }

}
