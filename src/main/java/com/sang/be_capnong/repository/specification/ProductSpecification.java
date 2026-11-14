package com.sang.be_capnong.repository.specification;

import com.sang.be_capnong.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> filterProducts(
            String status,
            Integer categoryId,
            String keyword,
            Integer shopId
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

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("productName")), pattern));
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
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy sản phẩm đang bán (APPROVED hoặc ACTIVE) và còn hàng
            predicates.add(root.get("status").in("APPROVED", "ACTIVE"));
            predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));

            // 2. Điều kiện khớp từ khóa theo tên nông sản hoặc danh mục
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("productName")), pattern);
                Predicate categoryMatch = cb.like(cb.lower(root.get("category").get("name")), pattern);
                predicates.add(cb.or(nameMatch, categoryMatch));
            }

            // 3. Loại trừ từ khóa con để tránh nhầm lẫn (ví dụ: bắp vs bắp cải)
            if (excludeKeyword != null && !excludeKeyword.isBlank()) {
                String excludePattern = "%" + excludeKeyword.trim().toLowerCase() + "%";
                predicates.add(cb.notLike(cb.lower(root.get("productName")), excludePattern));
                predicates.add(cb.notLike(cb.lower(root.get("category").get("name")), excludePattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
