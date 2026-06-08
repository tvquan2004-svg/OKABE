package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "users") // Ánh xạ đến bảng users
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của người dùng

    @Column(nullable = false, unique = true, length = 255) // Địa chỉ email (bắt buộc, duy nhất)
    private String email; // Địa chỉ email đăng nhập

    @Column(nullable = false, length = 100) // Tên người dùng (bắt buộc)
    private String username; // Tên hiển thị của người dùng

    @Column(nullable = false, length = 255) // Mật khẩu đã băm (bắt buộc)
    private String password; // Mật khẩu đã được mã hoá (BCrypt)

    @Column(name = "avatar_url", length = 500) // Đường dẫn ảnh đại diện
    private String avatarUrl; // URL ảnh đại diện của người dùng

    @Column(name = "is_active", nullable = false) // Trạng thái hoạt động (bắt buộc)
    @Builder.Default
    private Boolean isActive = true; // Tài khoản có đang hoạt động không

    @Column(nullable = false, length = 50) // Nhà cung cấp xác thực (bắt buộc)
    @Builder.Default
    private String provider = "LOCAL"; // Nhà cung cấp đăng nhập (LOCAL, GOOGLE, ...)

    @Column(name = "totp_secret", length = 64) // Khóa bí mật TOTP cho 2FA
    private String totpSecret; // Mã bí mật dùng cho xác thực hai yếu tố (Google Authenticator)

    @Column(name = "is_2fa_enabled", nullable = false) // Trạng thái bật 2FA (bắt buộc)
    @Builder.Default
    private Boolean is2faEnabled = false; // Xác thực hai yếu tố có được bật không
}
