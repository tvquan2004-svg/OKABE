package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEvent {
    private String type;
    private Long boardId;
    private Object payload;
    private Long actorId;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
