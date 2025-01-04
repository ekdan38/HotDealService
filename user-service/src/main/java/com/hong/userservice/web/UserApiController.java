package com.hong.userservice.web;

import com.hong.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "[UserApiController]")
public class UserApiController {

    private final UserRepository userRepository;

    // webClient 사용
    @GetMapping("/users/{username}")
    public ResponseEntity<Boolean> validateUser(@PathVariable("username") String username){

        String decodedUsername = new String(Base64.getUrlDecoder().decode(username), StandardCharsets.UTF_8);
        boolean userExists = userRepository.existsByUsername(decodedUsername);
        return ResponseEntity.ok(userExists);
    }

}
