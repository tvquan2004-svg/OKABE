package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required") // Tên workspace không được để trống
        @Size(max = 255, message = "Workspace name must not exceed 255 characters") // Giới hạn 255 ký tự
        String name, // Tên của không gian làm việc mới

        @Size(max = 1000, message = "Description must not exceed 1000 characters") // Giới hạn 1000 ký tự
        String description // Mô tả không gian làm việc (không bắt buộc)
) {}
