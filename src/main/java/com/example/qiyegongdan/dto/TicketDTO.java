package com.example.qiyegongdan.dto;

import com.example.qiyegongdan.enums.TicketPriority;
import com.example.qiyegongdan.enums.TicketStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * 工单响应 DTO（含关联信息）
 */
public record TicketDTO(
        Long id,
        String number,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long typeId,
        String typeName,
        UserBrief reporter,
        UserBrief assignee,
        String assignStrategy,
        Map<String, Object> customFields,
        java.time.LocalDateTime resolvedAt,
        java.time.LocalDateTime closedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        List<TicketLogDTO> logs
) {

    public record UserBrief(Long id, String username, String realName, String department) {}

    public record TicketLogDTO(String action, String detail, String operatorName, java.time.LocalDateTime createdAt) {}

    public record PageResult(List<TicketDTO> items, long total, int page, int size) {
        public static PageResult of(Page<TicketDTO> page) {
            return new PageResult(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
        }
    }
}