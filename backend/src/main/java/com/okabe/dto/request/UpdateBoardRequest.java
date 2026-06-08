package com.okabe.dto.request;

public record UpdateBoardRequest(
        String name, // Tên mới của bảng (không bắt buộc)
        String description, // Mô tả mới (không bắt buộc)
        String background, // Ảnh nền mới (không bắt buộc)
        Boolean isStarred, // Trạng thái gắn sao mới (không bắt buộc)
        Boolean isArchived // Trạng thái lưu trữ mới (không bắt buộc)
) {}
