package com.okabe.service.impl;

import com.okabe.dto.response.LunarDateResponse;
import com.okabe.dto.response.LunarMonthResponse;
import com.okabe.service.LunarDateService;
import com.okabe.util.LunarCalendarUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LunarDateServiceImpl implements LunarDateService {

    private static final List<Holiday> HOLIDAYS = List.of(
        // Solar holidays (fixed Gregorian date)
        new Holiday(1, 1, null, null, "Tết Dương Lịch"),
        new Holiday(30, 4, null, null, "Ngày Giải phóng miền Nam"),
        new Holiday(1, 5, null, null, "Quốc tế Lao động"),
        new Holiday(2, 9, null, null, "Quốc khánh"),

        // Lunar holidays
        new Holiday(null, null, 1, 1, "Tết Nguyên Đán"),
        new Holiday(null, null, 2, 1, "Tết Nguyên Đán"),
        new Holiday(null, null, 3, 1, "Tết Nguyên Đán"),
        new Holiday(null, null, 10, 3, "Giỗ Tổ Hùng Vương"),
        new Holiday(null, null, 15, 4, "Lễ Phật Đản"),
        new Holiday(null, null, 5, 5, "Tết Đoan Ngọ"),
        new Holiday(null, null, 15, 8, "Tết Trung Thu"),
        new Holiday(null, null, 23, 12, "Ông Táo chầu trời")
    );

    @Override
    public LunarDateResponse getLunarDate(LocalDate gregorianDate) {
        LunarCalendarUtil.LunarDate lunar = LunarCalendarUtil.solarToLunar(gregorianDate);
        return buildResponse(gregorianDate, lunar);
    }

    @Override
    public LunarMonthResponse getLunarMonth(int month, int year) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();

        List<LunarDateResponse> days = new ArrayList<>();
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = LocalDate.of(year, month, d);
            days.add(getLunarDate(date));
        }

        return LunarMonthResponse.builder()
            .month(month)
            .year(year)
            .days(days)
            .build();
    }

    private LunarDateResponse buildResponse(LocalDate gregorianDate, LunarCalendarUtil.LunarDate lunar) {
        int conventionalYear = LunarCalendarUtil.getConventionalYear(gregorianDate);

        String holidayName = findHoliday(gregorianDate, lunar);
        return LunarDateResponse.builder()
            .lunarDay(lunar.getDay())
            .lunarMonth(lunar.getMonth())
            .lunarYear(conventionalYear)
            .holiday(holidayName != null)
            .holidayName(holidayName)
            .build();
    }

    private String findHoliday(LocalDate gregorianDate, LunarCalendarUtil.LunarDate lunar) {
        for (Holiday h : HOLIDAYS) {
            if (h.solarDay != null && h.solarMonth != null) {
                if (gregorianDate.getDayOfMonth() == h.solarDay
                    && gregorianDate.getMonthValue() == h.solarMonth) {
                    return h.name;
                }
            }
            if (h.lunarDay != null && h.lunarMonth != null) {
                if (lunar.getDay() == h.lunarDay && lunar.getMonth() == h.lunarMonth) {
                    return h.name;
                }
            }
        }
        return null;
    }

    private record Holiday(Integer solarDay, Integer solarMonth,
                           Integer lunarDay, Integer lunarMonth, String name) {}
}
