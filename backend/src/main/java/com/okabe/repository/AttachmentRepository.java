package com.okabe.repository;

import com.okabe.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    // Tìm tất cả tệp đính kèm của card, sắp xếp theo thời gian tạo giảm dần
    List<Attachment> findByCardIdOrderByCreatedAtDesc(Long cardId);
    // Đếm số lượng tệp đính kèm của card
    int countByCardId(Long cardId);
}
