package com.okabe.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CardDependencyRequest(
    @NotEmpty(message = "Parent card IDs are required")
    List<Long> parentCardIds
) {}
