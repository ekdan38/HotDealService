package com.hong.hotdeal.service;

import com.hong.hotdeal.domain.User;
import com.hong.hotdeal.exception.ErrorCode;
import com.hong.hotdeal.exception.custom.UserException;
import com.hong.hotdeal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public User findById(Long userId){
        return userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }
}
