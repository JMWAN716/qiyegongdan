package com.example.qiyegongdan.dto;

public record TicketTypeDTO(
        Long id,
        String name,
        String description,
        Boolean active
) {
}