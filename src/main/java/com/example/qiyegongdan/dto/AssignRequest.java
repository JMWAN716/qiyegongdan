package com.example.qiyegongdan.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull(message = "请指定处理人") Long assigneeId,
        /** ROUND_ROBIN / LEAST_LOAD，仅自动分配时使用 */
        String strategy
) {
}