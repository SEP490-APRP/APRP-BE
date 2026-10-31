package com.sang.be_capnong.repository.specification;

import com.sang.be_capnong.entity.Notification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class NotificationSpecification {

    public static Specification<Notification> filterNotifications(String role, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null && !role.isBlank()) {
                String rolePattern = "%" + role.trim().toUpperCase() + "%";
                predicates.add(cb.like(cb.upper(root.get("receiverType")), rolePattern));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate messageMatch = cb.like(cb.lower(root.get("message")), pattern);
                predicates.add(cb.or(titleMatch, messageMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

