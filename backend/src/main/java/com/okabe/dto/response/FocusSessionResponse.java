package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionResponse {
    private Long id; // ID phiên tập trung
    private Long cardId; // ID thẻ đang làm việc
    private Long userId; // ID người dùng
    private String userName; // Tên người dùng
    private LocalDateTime startedAt; // Thời gian bắt đầu
    private LocalDateTime endedAt; // Thời gian kết thúc
    private Integer durationMinutes; // Thời lượng (phút)
    private boolean completed; // Đã hoàn thành?
    private int totalFocusMinutes; // Tổng phút tập trung
}
