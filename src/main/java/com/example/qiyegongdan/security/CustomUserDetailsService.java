package com.example.qiyegongdan.security;

import com.example.qiyegongdan.entity.User;
import com.example.qiyegongdan.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new UsernameNotFoundException("账号已被禁用: " + username);
        }
        return new UserPrincipal(user);
    }
}