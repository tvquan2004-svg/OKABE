package com.okabe.repository;

import com.okabe.entity.TemplateCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateCardRepository extends JpaRepository<TemplateCard, Long> {
}
