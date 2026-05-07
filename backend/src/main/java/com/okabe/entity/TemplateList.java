package com.okabe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "template_lists")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private BoardTemplate template;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer position;

    @OneToMany(mappedBy = "templateList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateCard> cards;
}
