package com.example.qiyegongdan.enums;

/**
 * 优先级
 */
public enum TicketPriority {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高"),
    URGENT("紧急");

    private final String label;

    TicketPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}