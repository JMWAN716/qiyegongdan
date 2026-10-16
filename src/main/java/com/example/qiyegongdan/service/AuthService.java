package com.example.qiyegongdan.service;

import com.example.qiyegongdan.repository.UserRepository;
import com.example.qiyegongdan.dto.AuthRequest;
import com.example.qiyegongdan.dto.AuthResponse;
import com.example.qiyegongdan.security.JwtUtil;
import com.example.qiyegongdan.security.UserPrincipal;
import com.example.qiyegongdan.entity.User;
import com.example.qiyegongdan.enums.RoleName;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(AuthRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();
        Set<String> roles = user.getRoles().stream().map(RoleName::name).collect(Collectors.toSet());
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), roles.stream().toList());
        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getRealName(), roles);
    }

    /**
     * 开放注册：默认赋予 REPORTER 角色
     */
    public AuthResponse register(String username, String password, String realName) {
        if (userRepository.existsByUsername(username)) {
            throw new com.example.qiyegongdan.exception.BusinessException("用户名已存在");
        }
        User user = new User(username, passwordEncoder.encode(password), realName);
        user.setRoles(Set.of(RoleName.REPORTER));
        user = userRepository.save(user);
        Set<String> roles = Set.of(RoleName.REPORTER.name());
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), roles.stream().toList());
        return AuthResponse.of(token, user.getId(), user.getUsername(), user.getRealName(), roles);
    }
}