package com.okabe.service;

import com.okabe.dto.response.LunarDateResponse;
import com.okabe.dto.response.LunarMonthResponse;

import java.time.LocalDate;

public interface LunarDateService {

    // Lấy thông tin ngày âm lịch từ ngày dương lịch
    LunarDateResponse getLunarDate(LocalDate gregorianDate);

    // Lấy thông tin tháng âm lịch theo tháng và năm
    LunarMonthResponse getLunarMonth(int month, int year);
}
