package com.hong.hotdeal.domain.entity;

import com.hong.hotdeal.domain.entity.base.TimeEntity;
import com.hong.hotdeal.domain.entity.status.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;
    // 추후 필요 하면 분리
    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private User(String username, String password, String name, String phoneNumber, String address, String email, Role role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.email = email;
        this.role = role;
    }

    // ==생성 메서드==
    public static User create(String username, String password, String name, String phoneNumber, String address, String email, Role role){
        return new User(username, password, name, phoneNumber, address, email, role);
    }
}
