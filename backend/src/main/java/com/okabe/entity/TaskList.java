package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lists")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;
}
