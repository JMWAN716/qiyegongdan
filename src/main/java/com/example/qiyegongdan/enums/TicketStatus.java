package com.example.qiyegongdan.enums;

/**
 * 工单全生命周期状态流转
 */
public enum TicketStatus {
    /** 新建/待分配 */
    OPEN("新建"),
    /** 已分配 */
    ASSIGNED("已分配"),
    /** 处理中 */
    IN_PROGRESS("处理中"),
    /** 已解决 */
    RESOLVED("已解决"),
    /** 已关闭 */
    CLOSED("已关闭"),
    /** 已取消 */
    CANCELED("已取消");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}