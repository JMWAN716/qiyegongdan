package com.example.qiyegongdan.dto;

import com.example.qiyegongdan.enums.TicketPriority;
import com.example.qiyegongdan.enums.TicketStatus;

public record TicketQueryRequest(
        String keyword,
        TicketStatus status,
        TicketPriority priority,
        Long typeId,
        Long assigneeId,
        Long reporterId,
        String createFrom,
        String createTo,
        Integer page,
        Integer size,
        String sortBy,
        String order
) {
}