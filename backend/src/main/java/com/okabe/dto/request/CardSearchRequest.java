package com.okabe.dto.request;

import com.okabe.entity.enums.Priority;
import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder
public record CardSearchRequest(
    String keyword,
    List<Long> assigneeIds,
    List<Long> labelIds,
    List<Priority> priorities,
    LocalDate dueDateFrom,
    LocalDate dueDateTo,
    Boolean isOverdue,
    Integer page,
    Integer size
) {
    public Integer getPage() {
        return page != null ? page : 0;
    }

    public Integer getSize() {
        return size != null ? size : 20;
    }
}
