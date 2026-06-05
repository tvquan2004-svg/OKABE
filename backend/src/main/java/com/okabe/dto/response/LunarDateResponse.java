package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LunarDateResponse {
    private int lunarDay;
    private int lunarMonth;
    private int lunarYear;

    @JsonProperty("isHoliday")
    private boolean holiday;

    private String holidayName;
}
