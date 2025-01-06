package com.hong.apigatewayservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class Controller {
    private final Environment env;

    @GetMapping("/jwt")
    public String jwt(){
        String property = env.getProperty("jwt.secret.key");
        log.info("jwt value = {}", property );
        return property;
    }
}
