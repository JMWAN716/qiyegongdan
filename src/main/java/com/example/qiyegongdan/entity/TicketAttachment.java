package com.example.qiyegongdan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 工单附件
 */
@Entity
@Table(name = "ticket_attachment")
@Getter
@Setter
@NoArgsConstructor
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false, length = 255)
    private String filename;

    /** 存储相对路径 */
    @Column(nullable = false, length = 500)
    private String storagePath;

    @Column(nullable = false, length = 100)
    private String contentType;

    private Long size;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}