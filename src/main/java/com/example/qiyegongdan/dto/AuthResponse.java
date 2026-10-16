package com.example.qiyegongdan.dto;

import java.util.Set;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String realName,
        Set<String> roles
) {
    public static AuthResponse of(String token, Long userId, String username, String realName, Set<String> roles) {
        return new AuthResponse(token, "Bearer", userId, username, realName, roles);
    }
}