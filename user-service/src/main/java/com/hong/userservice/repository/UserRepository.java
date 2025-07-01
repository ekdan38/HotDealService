package com.hong.userservice.repository;

import com.hong.userservice.domain.User;
import com.hong.userservice.dto.UserDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    @Query("SELECT new com.hong.userservice.dto.UserDto(u.id, u.username, u.password, u.role) " +
            "FROM User u " +
            "WHERE u.username = :username")
    UserDto findDtoByUsername(@Param("username") String username);

    boolean existsByEmail(String email);
}
