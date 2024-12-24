package com.hong.hotdeal.jwt;

import com.hong.hotdeal.domain.User;
import com.hong.hotdeal.repository.UserRepository;
import com.hong.hotdeal.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "[CustomUserDetailsService]")
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final AESUtil aesUtil;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            String encryptUsername = aesUtil.encrypt(username);
            User user = userRepository.findByUsername(encryptUsername);

            if(user != null){
                user.setDecryptUsername(aesUtil.decrypt(user.getUsername()));
                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority(user.getRole().toString()));
                return new CustomUserDetails(user, authorities);
            }
        } catch (Exception e) {
            log.error("error = {}", e.getMessage());
            throw new UsernameNotFoundException("일치하는 username 이 없습니다.");
        }
        return null;
    }
}
