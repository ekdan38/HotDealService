package com.hong.orderservice.client;


import com.hong.common.dto.UserCommonDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/user-service/users/{userId}")
    UserCommonDto getUserById(@PathVariable("userId") Long userId);
}
