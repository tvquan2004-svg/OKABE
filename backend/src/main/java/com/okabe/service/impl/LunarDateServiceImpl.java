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
        LunarCalendarUtil.LunarDate lunar = LunarCalendarUtil.solarToLunar(gregorianDate); // Chuyển đổi ngày dương lịch sang âm lịch
        return buildResponse(gregorianDate, lunar); // Xây dựng và trả về phản hồi
    }

    @Override
    public LunarMonthResponse getLunarMonth(int month, int year) {
        LocalDate firstDay = LocalDate.of(year, month, 1); // Ngày đầu tháng
        int daysInMonth = firstDay.lengthOfMonth(); // Số ngày trong tháng

        List<LunarDateResponse> days = new ArrayList<>(); // Khởi tạo danh sách ngày
        for (int d = 1; d <= daysInMonth; d++) { // Duyệt từng ngày trong tháng
            LocalDate date = LocalDate.of(year, month, d); // Tạo đối tượng ngày
            days.add(getLunarDate(date)); // Thêm thông tin âm lịch vào danh sách
        }

        return LunarMonthResponse.builder() // Xây dựng phản hồi
            .month(month) // Gán tháng
            .year(year) // Gán năm
            .days(days) // Gán danh sách ngày
            .build(); // Xây dựng LunarMonthResponse
    }

    private LunarDateResponse buildResponse(LocalDate gregorianDate, LunarCalendarUtil.LunarDate lunar) {
        int conventionalYear = LunarCalendarUtil.getConventionalYear(gregorianDate); // Lấy năm âm lịch quy ước

        String holidayName = findHoliday(gregorianDate, lunar); // Tìm tên ngày lễ
        return LunarDateResponse.builder()
            .lunarDay(lunar.getDay()) // Gán ngày âm lịch
            .lunarMonth(lunar.getMonth()) // Gán tháng âm lịch
            .lunarYear(conventionalYear) // Gán năm âm lịch
            .holiday(holidayName != null) // Đánh dấu có ngày lễ
            .holidayName(holidayName) // Gán tên ngày lễ
            .build(); // Xây dựng LunarDateResponse
    }

    private String findHoliday(LocalDate gregorianDate, LunarCalendarUtil.LunarDate lunar) {
        for (Holiday h : HOLIDAYS) { // Duyệt danh sách ngày lễ
            if (h.solarDay != null && h.solarMonth != null) { // Nếu là lễ dương lịch
                if (gregorianDate.getDayOfMonth() == h.solarDay // So khớp ngày
                    && gregorianDate.getMonthValue() == h.solarMonth) { // So khớp tháng
                    return h.name; // Trả về tên ngày lễ
                }
            }
            if (h.lunarDay != null && h.lunarMonth != null) { // Nếu là lễ âm lịch
                if (lunar.getDay() == h.lunarDay && lunar.getMonth() == h.lunarMonth) { // So khớp ngày tháng âm lịch
                    return h.name; // Trả về tên ngày lễ
                }
            }
        }
        return null; // Không tìm thấy ngày lễ
    }

    private record Holiday(Integer solarDay, Integer solarMonth,
                           Integer lunarDay, Integer lunarMonth, String name) {}
}
