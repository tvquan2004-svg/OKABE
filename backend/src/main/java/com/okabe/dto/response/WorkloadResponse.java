package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadResponse {

    private List<MemberWorkload> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberWorkload {
        private Long userId;
        private String userName;
        private String avatarUrl;
        private List<DayWorkload> workload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayWorkload {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private int cardCount;
        private double totalHours;
        private boolean isOverloaded;
    }
}
