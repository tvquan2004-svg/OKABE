package com.okabe.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private Long listId;
    private String title;
    private String description;
    private Integer position;
    private LocalDateTime dueDate;
    private String priority;
    private Boolean isArchived;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
