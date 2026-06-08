package com.okabe.service;

import com.okabe.dto.response.WorkloadResponse;
import com.okabe.security.UserPrincipal;

import java.time.LocalDate;

public interface WorkloadService {

    // Lấy dữ liệu workload heatmap cho tất cả thành viên trong workspace theo khoảng thời gian
    // Nhóm thẻ theo thành viên và ngày đến hạn, tính tổng số thẻ và giờ ước lượng mỗi ngày
    // Đánh dấu ngày QUÁ TẢI khi tổng giờ vượt quá 8
    WorkloadResponse getWorkload(Long workspaceId, LocalDate from, LocalDate to, UserPrincipal currentUser);
}
