package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignMemberRequest(
    @NotNull
    Long userId
) {}
