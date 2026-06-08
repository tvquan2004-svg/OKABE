package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadResponse {

    private List<MemberWorkload> members; // Danh sách khối lượng công việc theo thành viên

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberWorkload { // Khối lượng công việc của một thành viên
        private Long userId; // ID thành viên
        private String userName; // Tên thành viên
        private String avatarUrl; // Ảnh đại diện
        private List<DayWorkload> workload; // Chi tiết theo ngày
    }

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayWorkload { // Khối lượng công việc trong ngày
        @JsonFormat(pattern = "yyyy-MM-dd") // Định dạng ngày
        private LocalDate date; // Ngày
        private int cardCount; // Số thẻ
        private double totalHours; // Tổng số giờ
        private boolean isOverloaded; // Có quá tải không
    }
}
