package com.example.qiyegongdan.service;

import com.example.qiyegongdan.entity.OperationLog;
import com.example.qiyegongdan.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 安全审计日志服务：记录认证、权限和敏感操作
 */
@Service
public class AuditLogService {

    private final OperationLogRepository operationLogRepository;

    public AuditLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public void record(String module, String action, String detail, HttpServletRequest request) {
        try {
            OperationLog log = new OperationLog();
            String username = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.example.qiyegongdan.security.UserPrincipal p) {
                username = p.getUsername();
            }
            log.setUsername(username);
            log.setModule(module);
            log.setAction(action);
            log.setDetail(detail);
            log.setIp(resolveIp(request));
            operationLogRepository.save(log);
        } catch (Exception ignored) {
            // 审计不应影响主流程
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}