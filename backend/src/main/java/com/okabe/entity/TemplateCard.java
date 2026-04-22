package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_list_id", nullable = false)
    private TemplateList templateList;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer position;
}
