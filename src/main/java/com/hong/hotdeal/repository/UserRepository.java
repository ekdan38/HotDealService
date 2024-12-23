package com.hong.hotdeal.repository;

import com.hong.hotdeal.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);
    User findByUsername(String username);

    boolean existsByEmail(String email);

}
