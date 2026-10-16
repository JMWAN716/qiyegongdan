package com.example.qiyegongdan.enums;

/**
 * 自定义字段数据类型
 */
public enum FieldType {
    TEXT("文本"),
    NUMBER("数字"),
    SELECT("单选下拉"),
    DATE("日期"),
    BOOLEAN("布尔值");

    private final String label;

    FieldType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}