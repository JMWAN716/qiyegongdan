package com.example.qiyegongdan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 系统级安全审计日志（认证、权限、敏感操作）
 */
@Entity
@Table(name = "operation_log", indexes = {
        @Index(name = "idx_oplog_user", columnList = "username"),
        @Index(name = "idx_oplog_time", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 1000)
    private String detail;

    private String ip;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}