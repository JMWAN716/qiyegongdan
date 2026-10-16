package com.example.qiyegongdan.dto;

import java.util.Set;

public record UserDTO(
        Long id,
        String username,
        String realName,
        String email,
        String phone,
        String department,
        Boolean enabled,
        Set<String> roles
) {
}