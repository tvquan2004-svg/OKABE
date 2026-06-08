package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity // Đánh dấu là entity JPA
@Table(name = "email_verification_tokens") // Ánh xạ đến bảng email_verification_tokens
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của token xác thực email

    @Column(nullable = false, unique = true) // Giá trị token (bắt buộc, duy nhất)
    private String token; // Chuỗi token dùng để xác thực email

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER) // Một token thuộc về một người dùng
    @JoinColumn(nullable = false, name = "user_id") // Khoá ngoại đến bảng users
    private User user; // Người dùng cần xác thực email

    @Column(nullable = false) // Ngày hết hạn (bắt buộc)
    private LocalDateTime expiryDate; // Thời điểm token hết hạn

    public boolean isExpired() { // Kiểm tra token đã hết hạn chưa
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
