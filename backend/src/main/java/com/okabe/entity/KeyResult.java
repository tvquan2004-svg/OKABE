package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "key_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "objective_id", nullable = false)
    private Long objectiveId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "target_value", precision = 14, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "current_value", precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal currentValue = new BigDecimal("0.0000");

    @Column(length = 50)
    @Builder.Default
    private String unit = "percent";

    @Column(name = "linked_cards", columnDefinition = "TEXT")
    private String linkedCards;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
