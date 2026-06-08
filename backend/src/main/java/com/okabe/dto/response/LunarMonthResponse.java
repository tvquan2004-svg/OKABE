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
public class LunarMonthResponse {
    private int month; // Tháng
    private int year; // Năm
    private List<LunarDateResponse> days; // Danh sách ngày trong tháng âm lịch
}
