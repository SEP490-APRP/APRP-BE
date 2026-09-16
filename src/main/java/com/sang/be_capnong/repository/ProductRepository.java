package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByCategory_CategoryId(int categoryId);

    @Query("SELECT p FROM Product p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:categoryId IS NULL OR p.category.categoryId = :categoryId) AND " +
            "(:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findProducts(@Param("status") String status,
                               @Param("categoryId") Integer categoryId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND " +
            "(:status IS NULL OR p.status = :status)")
    Page<Product> findByShopIdAndOptionalStatus(@Param("shopId") int shopId,
                                                @Param("status") String status,
                                                Pageable pageable);
}
