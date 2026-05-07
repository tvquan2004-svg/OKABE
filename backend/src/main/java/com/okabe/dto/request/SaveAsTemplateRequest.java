package com.okabe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveAsTemplateRequest {
    @NotBlank(message = "Template name is required")
    private String name;
    private String description;
}
