package com.okabe.service;

import com.okabe.dto.response.LunarDateResponse;
import com.okabe.dto.response.LunarMonthResponse;

import java.time.LocalDate;

public interface LunarDateService {

    LunarDateResponse getLunarDate(LocalDate gregorianDate);

    LunarMonthResponse getLunarMonth(int month, int year);
}
