package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class LunarDateResponse {
    private int lunarDay; // Ngày âm lịch
    private int lunarMonth; // Tháng âm lịch
    private int lunarYear; // Năm âm lịch

    @JsonProperty("isHoliday") // Ánh xạ JSON: isHoliday
    private boolean holiday; // Có phải ngày lễ không

    private String holidayName; // Tên ngày lễ (nếu có)
}
