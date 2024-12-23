package com.hong.hotdeal.domain;

import com.hong.hotdeal.domain.base.TimeEntity;
import com.hong.hotdeal.domain.status.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private User(String username, String password, String name, String phoneNumber, Address address, String email, Role role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.email = email;
        this.role = role;
    }

    // ==생성 메서드==
    public static User create(String username, String password, String name, String phoneNumber, Address address, String email, Role role){
        return new User(username, password, name, phoneNumber, address, email, role);
    }

    // == username 복호화 ==
    public void setDecryptUsername(String decryptUsername){
        this.username = decryptUsername;
    }
}
