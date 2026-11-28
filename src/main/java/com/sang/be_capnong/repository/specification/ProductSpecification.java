package com.sang.be_capnong.repository.specification;

import com.sang.be_capnong.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filterProducts(
            String status,
            Integer categoryId,
            String keyword,
            Integer shopId
    ) {
        return filterProducts(status, categoryId, keyword, shopId, null, null, null, null);
    }

    public static Specification<Product> filterProducts(
            String status,
            Integer categoryId,
            String keyword,
            Integer shopId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String origin,
            Boolean isVietgap
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (shopId != null) {
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sellingPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sellingPrice"), maxPrice));
            }

            if (origin != null && !origin.isBlank()) {
                String originPattern = "%" + origin.trim().toLowerCase() + "%";
                Predicate originMatch = cb.like(cb.lower(root.get("origin")), originPattern);
                Predicate shopAddressMatch = cb.like(cb.lower(root.get("shop").get("address")), originPattern);
                predicates.add(cb.or(originMatch, shopAddressMatch));
            }

            if (Boolean.TRUE.equals(isVietgap)) {
                String vietgapPattern = "%vietgap%";
                Predicate licenseMatch = cb.like(cb.lower(root.get("shop").get("license")), vietgapPattern);
                Predicate achievementMatch = cb.like(cb.lower(root.get("shop").get("achievement")), vietgapPattern);
                predicates.add(cb.or(licenseMatch, achievementMatch));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("productName")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification phục vụ tìm kiếm nông sản bằng hình ảnh (Visual Search).
     * Chỉ lấy sản phẩm có trạng thái APPROVED hoặc ACTIVE, tồn kho > 0.
     * Khớp từ khóa theo tên sản phẩm hoặc tên danh mục, và loại trừ từ khóa con nếu có.
     */
    public static Specification<Product> searchForVisualSearch(String keyword, String excludeKeyword) {
        List<String> keywords = (keyword != null && !keyword.isBlank()) ? List.of(keyword) : List.of();
        return searchForVisualSearch(keywords, excludeKeyword, Collections.emptyList());
    }

    public static Specification<Product> searchForVisualSearch(List<String> keywords, String excludeKeyword) {
        return searchForVisualSearch(keywords, excludeKeyword, Collections.emptyList());
    }

    public static Specification<Product> searchForVisualSearch(
            List<String> keywords,
            String excludeKeyword,
            Collection<Integer> vectorMatchedProductIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy sản phẩm đã được duyệt trên sàn (APPROVED hoặc ACTIVE)
            predicates.add(root.get("status").in("APPROVED", "ACTIVE"));

            // Dùng 1 LEFT JOIN duy nhất tới bảng categories cho toàn bộ mệnh đề OR / NOT LIKE
            Join<Object, Object> categoryJoin = root.join("category", JoinType.LEFT);

            // 2. Điều kiện khớp: Khớp theo ID từ Vector Cosine Similarity HOẶC khớp từ khóa đồng nghĩa
            List<Predicate> matchOrPredicates = new ArrayList<>();

            if (vectorMatchedProductIds != null && !vectorMatchedProductIds.isEmpty()) {
                matchOrPredicates.add(root.get("productId").in(vectorMatchedProductIds));
            }

            if (keywords != null && !keywords.isEmpty()) {
                for (String kw : keywords) {
                    if (kw == null || kw.isBlank()) continue;
                    String pattern = "%" + kw.trim().toLowerCase() + "%";
                    Predicate nameMatch = cb.like(cb.lower(root.get("productName")), pattern);
                    Predicate categoryMatch = cb.like(cb.lower(categoryJoin.get("name")), pattern);
                    matchOrPredicates.add(cb.or(nameMatch, categoryMatch));
                }
            }

            if (!matchOrPredicates.isEmpty()) {
                predicates.add(cb.or(matchOrPredicates.toArray(new Predicate[0])));
            }

            // 3. Loại trừ từ khóa con để tránh nhầm lẫn (ví dụ: bắp vs bắp cải)
            if (excludeKeyword != null && !excludeKeyword.isBlank()) {
                String excludePattern = "%" + excludeKeyword.trim().toLowerCase() + "%";
                predicates.add(cb.notLike(cb.lower(root.get("productName")), excludePattern));
                predicates.add(cb.or(
                        cb.isNull(categoryJoin.get("name")),
                        cb.notLike(cb.lower(categoryJoin.get("name")), excludePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
