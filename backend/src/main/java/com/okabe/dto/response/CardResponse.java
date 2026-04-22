package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
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
    private LocalDateTime startDate;
    private String priority;
    private Boolean isArchived;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    
    // Phase 2 fields
    private List<LabelResponse> labels;
    private List<ChecklistResponse> checklists;
    private List<UserResponse> members;
    private List<AttachmentResponse> attachments;
}
