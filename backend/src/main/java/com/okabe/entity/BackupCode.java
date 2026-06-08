package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "user_backup_codes") // Ánh xạ đến bảng user_backup_codes
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupCode extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của mã dự phòng

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều mã dự phòng thuộc về một người dùng
    @JoinColumn(name = "user_id", nullable = false) // Khoá ngoại đến bảng users
    private User user; // Người dùng sở hữu mã dự phòng

    @Column(name = "code_hash", nullable = false, length = 255) // Mã băm của mã dự phòng (bắt buộc)
    private String codeHash; // Giá trị băm của mã dự phòng 2FA

    @Column(name = "is_used", nullable = false) // Trạng thái đã sử dụng (bắt buộc)
    @Builder.Default
    private Boolean isUsed = false; // Đánh dấu mã đã được sử dụng hay chưa
}
