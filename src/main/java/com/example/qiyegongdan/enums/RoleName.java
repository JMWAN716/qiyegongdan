package com.example.qiyegongdan.enums;

/**
 * 系统角色：管理权限 RBAC
 */
public enum RoleName {
    /** 系统管理员：全部权限 */
    ADMIN,
    /** 工单分配员/经理：可分配、查看统计 */
    MANAGER,
    /** 工单处理员：处理指派工单 */
    AGENT,
    /** 提交人/客户：创建和跟踪工单 */
    REPORTER;

    public static boolean isValid(String name) {
        if (name == null) {
            return false;
        }
        for (RoleName r : values()) {
            if (r.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}