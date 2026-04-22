package com.okabe.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isSystem;
    private List<TemplateListResponse> lists;
}
