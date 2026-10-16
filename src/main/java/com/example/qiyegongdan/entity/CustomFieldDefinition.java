package com.example.qiyegongdan.entity;

import com.example.qiyegongdan.enums.FieldType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 工单类型对应的自定义字段定义
 */
@Entity
@Table(name = "custom_field_def", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"type_id", "field_key"})
})
@Getter
@Setter
@NoArgsConstructor
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private TicketType ticketType;

    @Column(name = "field_key", nullable = false, length = 50)
    private String fieldKey;

    @Column(nullable = false, length = 50)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FieldType dataType;

    /** 是否必填 */
    private Boolean required = false;

    /** 单选下拉选项，逗号分隔 */
    @Column(length = 1000)
    private String options;
}