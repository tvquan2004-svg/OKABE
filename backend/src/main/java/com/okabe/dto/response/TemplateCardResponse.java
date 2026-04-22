package com.okabe.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCardResponse {
    private Long id;
    private String title;
    private String description;
    private Integer position;
}
