package com.hong.orderservice.client.user;

import com.hong.common.dto.UserCartDeleteRequestDto;
import com.hong.common.dto.UserCartDeleteResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @RequestMapping("user-service/protect/users/carts")
    UserCartDeleteResponseDto deleteUserCart(@RequestBody UserCartDeleteRequestDto requestDto);
}
