package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardAnalyticsResponse {

    private List<StatusStats> cardsByStatus;
    private List<PriorityStats> cardsByPriority;
    private List<MemberStats> cardsByMember;
    private List<BurndownData> burndown;
    private Float avgCompletionDays;
    private List<HeatmapData> activityHeatmap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusStats {
        private Long listId;
        private String listName;
        private Integer total;
        private Integer overdue;
        private Integer completedThisWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityStats {
        private String priority;
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberStats {
        private Long userId;
        private String username;
        private String avatarUrl;
        private Integer assignedCount;
        private Integer overdueCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownData {
        private LocalDate date;
        private Integer remaining;
        private Integer completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapData {
        private LocalDate date;
        private Integer count;
    }
}
