package com.okabe.dto.request;

public record UpdateListRequest(
        String name, // Tên mới của danh sách (không bắt buộc)
        Boolean isArchived // Trạng thái lưu trữ mới (không bắt buộc)
) {}
