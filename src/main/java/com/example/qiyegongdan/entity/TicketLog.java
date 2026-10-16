package com.example.qiyegongdan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 工单操作历史（状态流转日志）
 */
@Entity
@Table(name = "ticket_log")
@Getter
@Setter
@NoArgsConstructor
public class TicketLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String detail;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}