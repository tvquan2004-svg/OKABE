package com.okabe.dto.request;

import com.okabe.entity.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required") // Vai trò không được null
        Role role // Vai trò mới cho thành viên (OWNER, ADMIN, MEMBER)
) {}
