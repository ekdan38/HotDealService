package com.hong.userservice.jwt;

import com.hong.userservice.dto.UserDto;
import com.hong.userservice.repository.UserRepository;
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UserDto userDto = userRepository.findDtoByUsername(username);

            if (userDto == null) {
                log.error("error = {}", username);
                throw new UsernameNotFoundException("일치하는 username 이 없습니다.");
            }
            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(userDto.getRole().toString()));
            return new CustomUserDetails(userDto, authorities);

        } catch (Exception e) {
            log.error("error = {}", e.getMessage());
            throw new UsernameNotFoundException("인증 중 오류 발생");
        }
    }
}
