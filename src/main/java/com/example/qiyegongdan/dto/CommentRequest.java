package com.example.qiyegongdan.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank(message = "评论内容不能为空") String content,
        boolean internal
) {
}