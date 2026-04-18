package com.okabe.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListResponse {
    private Long id;
    private Long boardId;
    private String name;
    private Integer position;
    private List<CardResponse> cards;
}
