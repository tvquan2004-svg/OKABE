package com.okabe.dto.request;

import com.okabe.entity.enums.Priority;
import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder // Hỗ trợ builder pattern cho nhiều tham số
public record CardSearchRequest(
    String keyword, // Từ khoá tìm kiếm
    List<Long> assigneeIds, // Danh sách ID người được gán
    List<Long> labelIds, // Danh sách ID nhãn
    List<Priority> priorities, // Danh sách mức độ ưu tiên
    LocalDate dueDateFrom, // Ngày đến hạn từ
    LocalDate dueDateTo, // Ngày đến hạn đến
    Boolean isOverdue, // Lọc các thẻ quá hạn
    Integer page, // Số trang (mặc định 0)
    Integer size // Kích thước trang (mặc định 20)
) {
    public Integer getPage() { // Lấy số trang, mặc định 0 nếu null
        return page != null ? page : 0;
    }

    public Integer getSize() { // Lấy kích thước trang, mặc định 20 nếu null
        return size != null ? size : 20;
    }
}
