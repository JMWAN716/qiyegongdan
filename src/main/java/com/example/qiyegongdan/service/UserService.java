package com.example.qiyegongdan.service;

import com.example.qiyegongdan.dto.UserCreateRequest;
import com.example.qiyegongdan.dto.UserDTO;
import com.example.qiyegongdan.dto.UserUpdateRequest;
import com.example.qiyegongdan.entity.User;
import com.example.qiyegongdan.exception.BusinessException;
import com.example.qiyegongdan.exception.ResourceNotFoundException;
import com.example.qiyegongdan.repository.UserRepository;
import com.example.qiyegongdan.enums.RoleName;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO create(UserCreateRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BusinessException("用户名已存在");
        }
        if (req.roles() == null || req.roles().isEmpty()) {
            throw new BusinessException("至少需要一个角色");
        }
        User user = new User(req.username(), passwordEncoder.encode(req.password()), req.realName());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setDepartment(req.department());
        user.setRoles(req.roles());
        return toDTO(userRepository.save(user));
    }

    public UserDTO update(Long id, UserUpdateRequest req) {
        User user = getEntity(id);
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        if (req.realName() != null) user.setRealName(req.realName());
        if (req.email() != null) user.setEmail(req.email());
        if (req.phone() != null) user.setPhone(req.phone());
        if (req.department() != null) user.setDepartment(req.department());
        if (req.enabled() != null) user.setEnabled(req.enabled());
        if (req.roles() != null && !req.roles().isEmpty()) user.setRoles(req.roles());
        return toDTO(userRepository.save(user));
    }

    public void delete(Long id, User operator) {
        if (id.equals(operator.getId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        if (userRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    public UserDTO get(Long id) {
        return toDTO(getEntity(id));
    }

    public List<UserDTO> list(String roleFilter) {
        List<User> users = userRepository.findAll();
        if (roleFilter != null && !roleFilter.isBlank()) {
            RoleName role = RoleName.valueOf(roleFilter.toUpperCase());
            users = users.stream().filter(u -> u.getRoles().contains(role)).collect(Collectors.toList());
        }
        return users.stream().map(this::toDTO).toList();
    }

    public List<User> listAgents() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(RoleName.AGENT))
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()))
                .toList();
    }

    public User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    public UserDTO toDTO(User u) {
        return new UserDTO(u.getId(), u.getUsername(), u.getRealName(), u.getEmail(), u.getPhone(),
                u.getDepartment(), u.getEnabled(),
                u.getRoles().stream().map(RoleName::name).collect(Collectors.toSet()));
    }
}