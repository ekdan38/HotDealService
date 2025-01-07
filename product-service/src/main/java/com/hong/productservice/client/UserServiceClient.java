package com.hong.productservice.client;

import com.hong.common.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/user-service/users/{userId}")
    UserDto getUserById(@PathVariable("userId") Long userId);

}
