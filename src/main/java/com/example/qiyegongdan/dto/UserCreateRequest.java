package com.example.qiyegongdan.dto;

import com.example.qiyegongdan.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UserCreateRequest(
        @NotBlank(message = "登录名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "姓名不能为空") String realName,
        @Email String email,
        String phone,
        String department,
        @NotNull(message = "角色不能为空") Set<RoleName> roles
) {
}