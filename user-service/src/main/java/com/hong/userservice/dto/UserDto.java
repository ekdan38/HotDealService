package com.hong.userservice.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long userId;
    private String username;
    private String password;
    private String name;
    private String phoneNumber;
    private String city;
    private String street;
    private String zipCode;
    private String email;
    private String role;
}
