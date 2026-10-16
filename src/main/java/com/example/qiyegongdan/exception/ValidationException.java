package com.example.qiyegongdan.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = null;
    }

    public ValidationException(Map<String, String> errors) {
        super("参数校验失败");
        this.errors = errors;
    }
}