package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInfoResponse {
    private Long id;
    private Long listId;
    private String title;
    private String priority;
    private Boolean isArchived;
    private LocalDateTime dueDate;
    private String listName;
    private String boardName;
}
