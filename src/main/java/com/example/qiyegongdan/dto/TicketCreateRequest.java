package com.example.qiyegongdan.dto;

import com.example.qiyegongdan.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record TicketCreateRequest(
        @NotBlank(message = "标题不能为空") String title,
        String description,
        @NotNull(message = "工单类型不能为空") Long typeId,
        TicketPriority priority,
        /** true=自动分配，false=手动分配（需指定 assigneeId） */
        boolean autoAssign,
        Long assigneeId,
        /** 自定义字段值，key 对应字段 fieldKey */
        Map<String, Object> customFields
) {
}