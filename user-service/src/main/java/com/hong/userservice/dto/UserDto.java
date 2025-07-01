package com.hong.userservice.dto;

import com.hong.userservice.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private Long userId;
    private String username;
    private String password;
    private Role role;
}
