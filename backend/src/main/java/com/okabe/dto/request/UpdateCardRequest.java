package com.okabe.dto.request;

public record UpdateCardRequest(
        String title, // Tiêu đề mới (không bắt buộc)
        String description, // Mô tả mới (không bắt buộc)
        String priority, // Mức ưu tiên mới (không bắt buộc)
        String dueDate, // Hạn chót mới (không bắt buộc)
        String startDate, // Ngày bắt đầu mới (không bắt buộc)
        Boolean isArchived // Trạng thái lưu trữ mới (không bắt buộc)
) {}
