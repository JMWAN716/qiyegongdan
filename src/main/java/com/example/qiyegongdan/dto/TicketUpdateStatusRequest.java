package com.example.qiyegongdan.dto;

import com.example.qiyegongdan.enums.TicketStatus;

public record TicketUpdateStatusRequest(
        TicketStatus toStatus,
        String remark
) {
}