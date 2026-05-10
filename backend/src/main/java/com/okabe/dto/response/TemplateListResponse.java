package com.okabe.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateListResponse {
    private Long id;
    private String name;
    private Integer position;
    private List<TemplateCardResponse> cards;
}
