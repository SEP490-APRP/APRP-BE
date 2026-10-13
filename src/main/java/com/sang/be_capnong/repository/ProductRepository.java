package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByCategory_CategoryId(int categoryId);

    @Override
    @EntityGraph(attributePaths = {"category", "shop", "images"})
    Optional<Product> findById(Integer id);

    @EntityGraph(attributePaths = {"category", "shop", "images"})
    @Query("SELECT p FROM Product p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:categoryId IS NULL OR p.category.categoryId = :categoryId) AND " +
            "(:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findProducts(@Param("status") String status,
                               @Param("categoryId") Integer categoryId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"category", "shop", "images"})
    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND " +
            "(:status IS NULL OR p.status = :status)")
    Page<Product> findByShopIdAndOptionalStatus(@Param("shopId") int shopId,
                                                @Param("status") String status,
                                                Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.productId = :productId AND p.stockQuantity >= :quantity")
    int decreaseStockAtomic(@Param("productId") Integer productId, @Param("quantity") Integer quantity);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity WHERE p.productId = :productId")
    int increaseStockAtomic(@Param("productId") Integer productId, @Param("quantity") Integer quantity);
}
