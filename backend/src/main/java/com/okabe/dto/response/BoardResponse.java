package com.okabe.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private Integer position;
    private String background;
    private Boolean isStarred;
    private Boolean isArchived;
    private int listCount;
    private int totalCards;
    private LocalDateTime createdAt;
    private List<ListResponse> lists;
}
