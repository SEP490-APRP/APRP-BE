package com.sang.be_capnong.repository.specification;

import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.enums.MealType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BuildComboSpecification {

    public static Specification<BuildCombo> filterCombos(
            String status,
            MealType mealType,
            String region,
            String type,
            String keyword
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase()));
            }

            if (mealType != null) {
                predicates.add(cb.equal(root.get("mealType"), mealType));
            }

            if (region != null && !region.isBlank()) {
                String pattern = "%" + region.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("region")), pattern));
            }

            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("type")), type.trim().toLowerCase()));
            }

            if (keyword != null && !keyword.isBlank()) {
                String kwPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("comboName")), kwPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), kwPattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<BuildCombo> filterShopCombos(
            Integer shopId,
            String status,
            MealType mealType
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (shopId != null) {
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase()));
            }

            if (mealType != null) {
                predicates.add(cb.equal(root.get("mealType"), mealType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
