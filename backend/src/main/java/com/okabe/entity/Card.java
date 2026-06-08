package com.okabe.entity;

import com.okabe.entity.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity // Đánh dấu là entity JPA
@Table(name = "cards") // Ánh xạ đến bảng cards
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card extends BaseEntity {

    @Id // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tự động tăng
    private Long id; // ID duy nhất của thẻ

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều thẻ thuộc về một danh sách
    @JoinColumn(name = "list_id", nullable = false) // Khoá ngoại đến bảng lists
    private TaskList taskList; // Danh sách chứa thẻ

    @Column(nullable = false, length = 500) // Tiêu đề thẻ (bắt buộc)
    private String title; // Tiêu đề của thẻ công việc

    @Column(columnDefinition = "TEXT") // Mô tả thẻ dạng văn bản dài
    private String description; // Mô tả chi tiết công việc

    @Column(nullable = false) // Vị trí sắp xếp (bắt buộc)
    private Integer position; // Vị trí của thẻ trong danh sách

    @Column(name = "due_date") // Ngày đến hạn
    private LocalDateTime dueDate; // Hạn chót hoàn thành thẻ

    @Column(name = "start_date") // Ngày bắt đầu
    private LocalDateTime startDate; // Ngày bắt đầu thực hiện thẻ

    @Column(name = "estimated_hours", precision = 5, scale = 2) // Số giờ ước tính
    private BigDecimal estimatedHours; // Thời gian dự kiến để hoàn thành (giờ)

    @Column(name = "total_focus_minutes") // Tổng số phút tập trung
    @Builder.Default
    private Integer totalFocusMinutes = 0; // Tổng thời gian tập trung đã ghi nhận (phút)

    @Enumerated(EnumType.STRING) // Lưu enum dạng chuỗi
    @Column(nullable = false, length = 20) // Mức độ ưu tiên (bắt buộc)
    @Builder.Default
    private Priority priority = Priority.MEDIUM; // Mức độ ưu tiên của thẻ

    @Column(name = "is_archived", nullable = false) // Trạng thái lưu trữ (bắt buộc)
    @Builder.Default
    private Boolean isArchived = false; // Thẻ có bị lưu trữ không

    @Column(name = "notification_sent", nullable = false) // Đã gửi thông báo nhắc nhở (bắt buộc)
    @Builder.Default
    private Boolean notificationSent = false; // Đã gửi thông báo đến hạn chưa

    @ManyToOne(fetch = FetchType.LAZY) // Nhiều thẻ được tạo bởi một người dùng
    @JoinColumn(name = "created_by", nullable = false) // Khoá ngoại đến bảng users
    private User createdBy; // Người tạo thẻ

    @ManyToMany // Nhiều-nhiều giữa thẻ và nhãn
    @JoinTable( // Bảng trung gian card_labels
        name = "card_labels",
        joinColumns = @JoinColumn(name = "card_id"), // Khoá ngoại đến bảng cards
        inverseJoinColumns = @JoinColumn(name = "label_id") // Khoá ngoại đến bảng labels
    )
    @Builder.Default
    private Set<Label> labels = new HashSet<>(); // Các nhãn được gán cho thẻ

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true) // Một thẻ có nhiều checklist
    @OrderBy("position ASC") // Sắp xếp theo vị trí tăng dần
    @Builder.Default
    private List<Checklist> checklists = new ArrayList<>(); // Danh sách các checklist của thẻ

    @ManyToMany // Nhiều-nhiều giữa thẻ và thành viên
    @JoinTable( // Bảng trung gian card_members
        name = "card_members",
        joinColumns = @JoinColumn(name = "card_id"), // Khoá ngoại đến bảng cards
        inverseJoinColumns = @JoinColumn(name = "user_id") // Khoá ngoại đến bảng users
    )
    @Builder.Default
    private Set<User> members = new HashSet<>(); // Các thành viên được gán vào thẻ

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true) // Một thẻ có nhiều tệp đính kèm
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>(); // Danh sách tệp đính kèm của thẻ

    @Column(name = "parent_ids", columnDefinition = "JSON") // ID các thẻ cha (lưu JSON)
    private String parentIds; // Danh sách ID thẻ cha (quan hệ phụ thuộc)
}
