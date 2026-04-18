package com.okabe.dto.request;

import com.okabe.entity.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}
