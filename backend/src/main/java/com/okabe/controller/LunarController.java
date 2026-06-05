package com.okabe.controller;

import com.okabe.dto.response.ApiResponse;
import com.okabe.dto.response.LunarDateResponse;
import com.okabe.dto.response.LunarMonthResponse;
import com.okabe.service.LunarDateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/lunar")
@RequiredArgsConstructor
@Tag(name = "Lunar Calendar", description = "Vietnamese lunar calendar and holiday APIs")
public class LunarController {

    private final LunarDateService lunarDateService;

    @GetMapping
    @Operation(summary = "Get lunar date for a Gregorian date")
    public ResponseEntity<ApiResponse<LunarDateResponse>> getLunarDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(lunarDateService.getLunarDate(date)));
    }

    @GetMapping("/month")
    @Operation(summary = "Get all lunar dates for a Gregorian month")
    public ResponseEntity<ApiResponse<LunarMonthResponse>> getLunarMonth(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(lunarDateService.getLunarMonth(month, year)));
    }
}
