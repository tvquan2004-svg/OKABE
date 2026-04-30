package com.okabe.repository;

import com.okabe.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Page<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
}
