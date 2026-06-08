package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class FocusStatsResponse {
    private int todayMinutes; // Phút tập trung hôm nay
    private int weekMinutes; // Phút tập trung tuần này
    private int monthMinutes; // Phút tập trung tháng này
    private int weekChangePercent; // Phần trăm thay đổi so với tuần trước
    private List<DailyFocus> dailyBreakdown; // Chi tiết theo ngày
    private List<TopCard> topCards; // Thẻ có nhiều thời gian tập trung nhất

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyFocus { // Tập trung theo ngày
        private String date; // Ngày (chuỗi)
        private int minutes; // Số phút
    }

    @Data
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCard { // Thẻ tập trung nhiều nhất
        private Long cardId; // ID thẻ
        private String cardTitle; // Tiêu đề thẻ
        private int sessions; // Số phiên
        private int totalMinutes; // Tổng số phút
    }
}
