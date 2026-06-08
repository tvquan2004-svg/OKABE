package com.okabe.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Size(max = 255, message = "Workspace name must not exceed 255 characters") // Giới hạn 255 ký tự
        String name, // Tên mới của workspace (không bắt buộc)

        @Size(max = 1000, message = "Description must not exceed 1000 characters") // Giới hạn 1000 ký tự
        String description // Mô tả mới của workspace (không bắt buộc)
) {}
