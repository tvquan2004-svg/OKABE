package com.okabe.entity;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Tự động tạo equals và hashCode
public class WorkspaceMemberId implements Serializable { // Lớp khoá chính hợp thành
    private Long workspaceId; // Thành phần khoá: ID không gian làm việc
    private Long userId; // Thành phần khoá: ID người dùng
}
