package com.okabe.dto.response;

import java.time.LocalDate;

public record StandupSummary(
    Long userId, // ID người dùng
    String userName, // Tên người dùng
    String avatarUrl, // Ảnh đại diện
    LocalDate date, // Ngày
    String done, // Việc đã làm
    String inProgress, // Việc đang làm
    String blocked // Việc bị chặn
) {
}
