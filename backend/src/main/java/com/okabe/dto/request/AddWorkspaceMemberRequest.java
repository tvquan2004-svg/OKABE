package com.okabe.dto.request;

import com.okabe.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddWorkspaceMemberRequest(
        @NotBlank(message = "Email is required") // Email không được để trống
        @Email(message = "Invalid email format") // Kiểm tra định dạng email hợp lệ
        String email, // Email của người dùng được mời vào workspace

        @NotNull(message = "Role is required") // Vai trò không được null
        Role role // Vai trò sẽ gán cho thành viên (OWNER, ADMIN, MEMBER)
) {}
