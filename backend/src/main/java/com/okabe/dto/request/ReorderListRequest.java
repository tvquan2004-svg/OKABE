package com.okabe.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderListRequest(
        @NotNull(message = "Ordered list IDs are required") // Danh sách ID không được null
        List<Long> orderedIds // Danh sách ID danh sách theo thứ tự mới
) {}
