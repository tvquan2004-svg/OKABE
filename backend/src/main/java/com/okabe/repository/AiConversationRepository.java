package com.okabe.repository;

import com.okabe.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    // Tìm tất cả cuộc hội thoại của user, sắp xếp theo thời gian cập nhật giảm dần (có phân trang)
    Page<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    // Kiểm tra cuộc hội thoại có tồn tại với id và userId hay không
    boolean existsByIdAndUserId(Long id, Long userId);
}
