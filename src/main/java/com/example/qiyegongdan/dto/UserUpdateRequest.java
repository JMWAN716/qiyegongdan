package com.example.qiyegongdan.dto;

import com.example.qiyegongdan.enums.RoleName;

import java.util.Set;

public record UserUpdateRequest(
        String password,
        String realName,
        String email,
        String phone,
        String department,
        Boolean enabled,
        Set<RoleName> roles
) {
}