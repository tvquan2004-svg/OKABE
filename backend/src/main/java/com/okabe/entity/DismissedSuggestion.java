package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dismissed_suggestions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DismissedSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "dismissed_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dismissedAt = LocalDateTime.now();
}
