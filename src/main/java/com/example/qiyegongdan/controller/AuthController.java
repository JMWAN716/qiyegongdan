package com.example.qiyegongdan.controller;

import com.example.qiyegongdan.dto.ApiResponse;
import com.example.qiyegongdan.dto.AuthRequest;
import com.example.qiyegongdan.dto.AuthResponse;
import com.example.qiyegongdan.service.AuditLogService;
import com.example.qiyegongdan.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest http) {
        AuthResponse resp = authService.login(request);
        auditLogService.record("认证", "登录", "用户 " + request.username() + " 登录成功", http);
        return ApiResponse.ok("登录成功", resp);
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody AuthRequest request, HttpServletRequest http) {
        AuthResponse resp = authService.register(request.username(), request.password(),
                request.username());
        auditLogService.record("认证", "注册", "新用户注册: " + request.username(), http);
        return ApiResponse.ok("注册成功", resp);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me() {
        return ApiResponse.ok(Map.of("userId", "use /api/users/me for details"));
    }
}