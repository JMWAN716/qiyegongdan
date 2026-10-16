package com.example.qiyegongdan.controller;

import com.example.qiyegongdan.dto.ApiResponse;
import com.example.qiyegongdan.dto.UserCreateRequest;
import com.example.qiyegongdan.dto.UserDTO;
import com.example.qiyegongdan.dto.UserUpdateRequest;
import com.example.qiyegongdan.entity.User;
import com.example.qiyegongdan.security.SecurityUtils;
import com.example.qiyegongdan.service.AuditLogService;
import com.example.qiyegongdan.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理（仅 ADMIN 可管理；所有登录用户可查询列表/自身信息）
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<UserDTO>> list(@RequestParam(required = false) String role) {
        return ApiResponse.ok(userService.list(role));
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ApiResponse<List<UserDTO>> agents() {
        return ApiResponse.ok(userService.list("AGENT"));
    }

    @GetMapping("/me")
    public ApiResponse<UserDTO> me() {
        User u = SecurityUtils.requireCurrentUser();
        return ApiResponse.ok(userService.toDTO(u));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDTO> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDTO> create(@Valid @RequestBody UserCreateRequest req, HttpServletRequest http) {
        UserDTO dto = userService.create(req);
        auditLogService.record("用户管理", "创建用户", "创建用户 " + req.username(), http);
        return ApiResponse.ok("创建成功", dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDTO> update(@PathVariable Long id, @RequestBody UserUpdateRequest req, HttpServletRequest http) {
        UserDTO dto = userService.update(id, req);
        auditLogService.record("用户管理", "更新用户", "更新用户 id=" + id, http);
        return ApiResponse.ok("更新成功", dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest http) {
        User operator = SecurityUtils.requireCurrentUser();
        userService.delete(id, operator);
        auditLogService.record("用户管理", "删除用户", "删除用户 id=" + id, http);
        return ApiResponse.ok("删除成功", null);
    }
}