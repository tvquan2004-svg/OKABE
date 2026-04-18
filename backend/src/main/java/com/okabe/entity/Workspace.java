package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
