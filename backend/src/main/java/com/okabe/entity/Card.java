package com.okabe.entity;

import com.okabe.entity.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private TaskList taskList;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
