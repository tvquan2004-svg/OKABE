package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignMemberRequest(
    @NotNull // ID người dùng không được null
    Long userId // ID của người dùng cần gán vào thẻ
) {}
