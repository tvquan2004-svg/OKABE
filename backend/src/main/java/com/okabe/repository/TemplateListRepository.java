package com.okabe.repository;

import com.okabe.entity.TemplateList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateListRepository extends JpaRepository<TemplateList, Long> {
}
