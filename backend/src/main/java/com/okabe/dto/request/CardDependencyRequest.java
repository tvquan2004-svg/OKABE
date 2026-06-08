package com.okabe.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CardDependencyRequest(
    @NotEmpty(message = "Parent card IDs are required") // Danh sách không được rỗng
    List<Long> parentCardIds // Danh sách ID các thẻ cha (phụ thuộc)
) {}
