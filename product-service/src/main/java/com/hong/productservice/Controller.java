package com.hong.productservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    @GetMapping("/")
    public String check(@RequestHeader("X-User-Id") Long userId,
                        @RequestHeader("X-User-Role") String userRole){
        return "userId = " + userId + "userRole = " + userRole;
    }
}
