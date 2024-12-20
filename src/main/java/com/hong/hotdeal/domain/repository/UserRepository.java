package com.hong.hotdeal.domain.repository;

import com.hong.hotdeal.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
