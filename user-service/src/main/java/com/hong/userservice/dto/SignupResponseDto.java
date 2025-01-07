package com.hong.userservice.dto;

import com.hong.userservice.domain.Address;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignupResponseDto {

    private Long userId;
    private String username;
    private String name;
    private String phoneNumber;
    private String email;
    private Address address;

}

