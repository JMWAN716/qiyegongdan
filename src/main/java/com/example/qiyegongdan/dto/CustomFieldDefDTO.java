package com.example.qiyegongdan.dto;

import com.example.qiyegongdan.enums.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomFieldDefDTO(
        Long id,
        @NotBlank(message = "字段标识不能为空") String fieldKey,
        @NotBlank(message = "字段名称不能为空") String label,
        @NotNull(message = "数据类型不能为空") FieldType dataType,
        boolean required,
        String options
) {
}