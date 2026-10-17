package com.sang.be_capnong.repository.specification;

import com.sang.be_capnong.entity.Blog;
import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BlogSpecification {

    public static Specification<Blog> filterBlogs(
            BlogStatus status,
            BlogCategory category,
            String keyword,
            Integer shopId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (shopId != null) {
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate shopNameMatch = cb.like(cb.lower(root.get("shop").get("shopName")), pattern);
                Predicate authorMatch = cb.like(cb.lower(root.get("shop").get("fullName")), pattern);
                predicates.add(cb.or(titleMatch, shopNameMatch, authorMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
