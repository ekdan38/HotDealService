package com.hong.userservice.web;

import com.hong.common.dto.UserDto;
import com.hong.common.exception.ErrorCode;
import com.hong.common.exception.custom.UserException;
import com.hong.userservice.domain.User;
import com.hong.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[UserApiController]")
@RequestMapping("/user-service")
public class UserApiController {

    private final UserRepository userRepository;

    // webClient 사용
    @GetMapping("/users/validate/{username}")
    public ResponseEntity<Boolean> validateUser(@PathVariable("username") String username){

        String decodedUsername = new String(Base64.getUrlDecoder().decode(username), StandardCharsets.UTF_8);
        boolean userExists = userRepository.existsByUsername(decodedUsername);
        return ResponseEntity.ok(userExists);
    }

    // FeignClient
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("userId") Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        UserDto userDto = new UserDto(
                user.getId(),
                user.getName(),
                user.getPhoneNumber(),
                user.getAddress().getCity(),
                user.getAddress().getStreet(),
                user.getAddress().getZipCode(),
                user.getEmail(),
                user.getRole().name());

        return ResponseEntity.ok().body(userDto);
    }

}
