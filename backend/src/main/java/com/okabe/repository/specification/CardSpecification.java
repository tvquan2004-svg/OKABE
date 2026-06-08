package com.okabe.repository.specification;

import com.okabe.dto.request.CardSearchRequest;
import com.okabe.entity.Card;
import com.okabe.entity.Label;
import com.okabe.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CardSpecification {

    // Tạo Specification để lọc thẻ theo các tiêu chí tìm kiếm (board, keyword, người được gán, nhãn, ưu tiên, ngày đến hạn, quá hạn)
    public static Specification<Card> filterByRequest(Long boardId, CardSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lọc theo board (thông qua taskList)
            predicates.add(cb.equal(root.get("taskList").get("board").get("id"), boardId));
            
            // Chỉ lấy thẻ chưa lưu trữ
            predicates.add(cb.equal(root.get("isArchived"), false));

            // Tìm kiếm theo từ khóa (tiêu đề HOẶC mô tả)
            if (request.keyword() != null && !request.keyword().isBlank()) {
                String keyword = "%" + request.keyword().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword)
                ));
            }

            // Lọc theo người được gán
            if (request.assigneeIds() != null && !request.assigneeIds().isEmpty()) {
                Join<Card, User> membersJoin = root.join("members");
                predicates.add(membersJoin.get("id").in(request.assigneeIds()));
            }

            // Lọc theo nhãn
            if (request.labelIds() != null && !request.labelIds().isEmpty()) {
                Join<Card, Label> labelsJoin = root.join("labels");
                predicates.add(labelsJoin.get("id").in(request.labelIds()));
            }

            // Lọc theo mức ưu tiên
            if (request.priorities() != null && !request.priorities().isEmpty()) {
                predicates.add(root.get("priority").in(request.priorities()));
            }

            // Lọc theo khoảng ngày đến hạn
            if (request.dueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), request.dueDateFrom().atStartOfDay()));
            }
            if (request.dueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), request.dueDateTo().atTime(23, 59, 59)));
            }

            // Lọc thẻ quá hạn
            if (Boolean.TRUE.equals(request.isOverdue())) {
                predicates.add(cb.lessThan(root.get("dueDate"), LocalDateTime.now()));
            }

            // Tránh trùng lặp khi JOIN
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
