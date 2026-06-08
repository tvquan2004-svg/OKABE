package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponse {
    private String type; // Loại phản hồi lệnh
    private String message; // Thông báo kết quả
    private Object data; // Dữ liệu bổ sung (nếu có)
}
