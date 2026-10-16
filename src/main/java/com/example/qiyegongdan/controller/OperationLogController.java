package com.example.qiyegongdan.controller;

import com.example.qiyegongdan.dto.ApiResponse;
import com.example.qiyegongdan.entity.OperationLog;
import com.example.qiyegongdan.repository.OperationLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作审计日志查询（ADMIN）
 */
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')")
public class OperationLogController {

    private final OperationLogRepository logRepository;

    public OperationLogController(OperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping
    public ApiResponse<Page<OperationLog>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (username != null && !username.isBlank()) ps.add(cb.like(root.get("username"), "%" + username + "%"));
            if (module != null && !module.isBlank()) ps.add(cb.equal(root.get("module"), module));
            if (action != null && !action.isBlank()) ps.add(cb.like(root.get("action"), "%" + action + "%"));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<OperationLog> result = logRepository.findAll(spec,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.ok(result);
    }
}