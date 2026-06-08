package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder // Hỗ trợ builder pattern
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEvent {
    private String type; // Loại sự kiện (VD: card_updated, list_moved)
    private Long boardId; // ID bảng liên quan
    private Object payload; // Dữ liệu sự kiện
    private Long actorId; // ID người thực hiện
    @Builder.Default
    private Instant timestamp = Instant.now(); // Thời điểm sự kiện xảy ra
}
