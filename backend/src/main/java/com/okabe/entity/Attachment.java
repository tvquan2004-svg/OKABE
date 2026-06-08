package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Đánh dấu là entity JPA
@Table(name = "attachments") // Ánh xạ đến bảng attachments
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của tệp đính kèm

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều tệp đính kèm thuộc về một thẻ
    @JoinColumn(name = "card_id", nullable = false) // Khoá ngoại đến bảng cards
    private Card card; // Thẻ chứa tệp đính kèm

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều tệp đính kèm được tải lên bởi một người dùng
    @JoinColumn(name = "uploaded_by", nullable = false) // Khoá ngoại đến bảng users
    private User uploadedBy; // Người dùng đã tải tệp lên

    @Column(nullable = false, length = 255) // Tên tệp gốc (bắt buộc)
    private String filename; // Tên tệp gốc khi tải lên

    @Column(name = "storage_key", nullable = false, length = 500) // Khóa lưu trữ trên hệ thống file (bắt buộc)
    private String storageKey; // Đường dẫn/khoá lưu trữ trên S3/cloud

    @Column(name = "file_size") // Kích thước tệp (byte)
    private Long fileSize; // Kích thước tệp tính bằng byte

    @Column(name = "mime_type", length = 100) // Loại MIME của tệp
    private String mimeType; // Kiểu MIME (VD: image/png, application/pdf)
}
