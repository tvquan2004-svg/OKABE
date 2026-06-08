package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class BoardAnalyticsResponse {

    private List<StatusStats> cardsByStatus; // Thống kê thẻ theo trạng thái (danh sách)
    private List<PriorityStats> cardsByPriority; // Thống kê thẻ theo mức độ ưu tiên
    private List<MemberStats> cardsByMember; // Thống kê thẻ theo thành viên
    private List<BurndownData> burndown; // Dữ liệu biểu đồ burn-down
    private Float avgCompletionDays; // Số ngày hoàn thành trung bình
    private List<HeatmapData> activityHeatmap; // Dữ liệu heatmap hoạt động

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusStats { // Thống kê theo danh sách trạng thái
        private Long listId; // ID danh sách
        private String listName; // Tên danh sách
        private Integer total; // Tổng số thẻ
        private Integer overdue; // Số thẻ quá hạn
        private Integer completedThisWeek; // Số thẻ hoàn thành trong tuần
    }

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityStats { // Thống kê theo mức độ ưu tiên
        private String priority; // Mức độ ưu tiên
        private Integer count; // Số lượng thẻ
    }

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberStats { // Thống kê theo thành viên
        private Long userId; // ID thành viên
        private String username; // Tên thành viên
        private String avatarUrl; // URL ảnh đại diện
        private Integer assignedCount; // Số thẻ được gán
        private Integer overdueCount; // Số thẻ quá hạn
    }

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownData { // Dữ liệu burn-down chart
        private LocalDate date; // Ngày
        private Integer remaining; // Số thẻ còn lại
        private Integer completed; // Số thẻ đã hoàn thành
    }

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapData { // Dữ liệu heatmap
        private LocalDate date; // Ngày
        private Integer count; // Số lượng hoạt động
    }
}
