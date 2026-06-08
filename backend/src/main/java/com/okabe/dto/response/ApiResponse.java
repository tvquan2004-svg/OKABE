package com.okabe.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Không bao gồm các trường null trong JSON
public class ApiResponse<T> {

    private boolean success; // Trạng thái thành công của API
    private String message; // Thông báo kết quả
    private T data; // Dữ liệu trả về (kiểu generic)
    private String errorCode; // Mã lỗi (nếu có)
    private List<FieldError> errors; // Danh sách lỗi field (nếu có)

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now(); // Thời gian phản hồi

    public static <T> ApiResponse<T> success(T data) { // Tạo phản hồi thành công với dữ liệu
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) { // Tạo phản hồi thành công với dữ liệu và thông báo
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) { // Tạo phản hồi lỗi
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, List<FieldError> errors) { // Tạo phản hồi lỗi với danh sách lỗi field
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .errors(errors)
                .build();
    }

    @Getter
    @Builder // Hỗ trợ builder pattern
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError { // Lỗi validate của một field cụ thể
        private String field; // Tên field bị lỗi
        private String message; // Thông báo lỗi
    }
}
