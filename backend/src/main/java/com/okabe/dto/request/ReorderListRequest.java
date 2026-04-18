package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderListRequest(
        @NotNull(message = "Ordered list IDs are required")
        List<Long> orderedIds
) {}
