package com.example.qiyegongdan.security;

import com.example.qiyegongdan.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 获取当前登录用户
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser();
        }
        return null;
    }

    public static User requireCurrentUser() {
        User user = getCurrentUser();
        if (user == null) {
            throw new com.example.qiyegongdan.exception.BusinessException("未登录或登录已过期");
        }
        return user;
    }

    public static boolean hasRole(Role role) {
        User user = getCurrentUser();
        return user != null && user.getRoles().contains(role);
    }

    public enum Role {
        ADMIN, MANAGER, AGENT, REPORTER
    }
}