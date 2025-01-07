package com.hong.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCommonDto {
    private Long userId;
    private String name;
    private String phoneNumber;
    private String city;
    private String street;
    private String zipCode;
    private String email;
    private String role;
}
