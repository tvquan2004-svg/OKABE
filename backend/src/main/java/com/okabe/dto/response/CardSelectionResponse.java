package com.okabe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSelectionResponse {
    private Long id;
    private String title;
    private Long boardId;
    private String boardName;
    private String listName;
}
