package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity // Đánh dấu là entity JPA
@Table(name = "comments") // Ánh xạ đến bảng comments
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của bình luận

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều bình luận thuộc về một thẻ
    @JoinColumn(name = "card_id", nullable = false) // Khoá ngoại đến bảng cards
    private Card card; // Thẻ được bình luận

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều bình luận thuộc về một người dùng
    @JoinColumn(name = "user_id", nullable = false) // Khoá ngoại đến bảng users
    private User user; // Người dùng viết bình luận

    @Column(nullable = false, columnDefinition = "TEXT") // Nội dung bình luận (bắt buộc, dạng văn bản dài)
    private String content; // Nội dung bình luận

    @Column(name = "is_edited", nullable = false) // Trạng thái đã chỉnh sửa (bắt buộc)
    @Builder.Default
    private Boolean isEdited = false; // Bình luận đã được chỉnh sửa chưa

    @ManyToMany // Nhiều-nhiều giữa bình luận và người dùng được đề cập
    @JoinTable( // Bảng trung gian comment_mentions
        name = "comment_mentions",
        joinColumns = @JoinColumn(name = "comment_id"), // Khoá ngoại đến bảng comments
        inverseJoinColumns = @JoinColumn(name = "user_id") // Khoá ngoại đến bảng users
    )
    @Builder.Default
    private Set<User> mentions = new HashSet<>(); // Danh sách người dùng được đề cập trong bình luận
}
