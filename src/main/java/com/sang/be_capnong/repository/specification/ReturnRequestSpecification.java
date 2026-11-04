package com.sang.be_capnong.repository.specification;

import com.sang.be_capnong.entity.ReturnRequest;
import com.sang.be_capnong.enums.ReturnStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ReturnRequestSpecification {

    public static Specification<ReturnRequest> filterDisputes(ReturnStatus status, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";

                Predicate reasonMatch = cb.like(cb.lower(root.get("reason")), pattern);

                Predicate buyerMatch = cb.like(cb.lower(root.get("buyer").get("fullName")), pattern);

                Predicate shopNameMatch = cb.like(cb.lower(root.get("shop").get("shopName")), pattern);
                Predicate shopFullNameMatch = cb.like(cb.lower(root.get("shop").get("fullName")), pattern);

                Predicate orderIdMatch = cb.like(root.get("orderDetail").get("order").get("orderId").as(String.class), pattern);

                predicates.add(cb.or(reasonMatch, buyerMatch, shopNameMatch, shopFullNameMatch, orderIdMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

