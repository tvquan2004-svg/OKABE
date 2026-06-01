package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusStatsResponse {
    private int todayMinutes;
    private int weekMinutes;
    private int monthMinutes;
    private int weekChangePercent;
    private List<DailyFocus> dailyBreakdown;
    private List<TopCard> topCards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyFocus {
        private String date;
        private int minutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCard {
        private Long cardId;
        private String cardTitle;
        private int sessions;
        private int totalMinutes;
    }
}
