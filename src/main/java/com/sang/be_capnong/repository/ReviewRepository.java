package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByShop_Id(int shopId);
    Page<Review> findByShop_Id(int shopId, Pageable pageable);

    List<Review> findByProduct_ProductId(int productId);
    Page<Review> findByProduct_ProductId(int productId, Pageable pageable);

    List<Review> findByMysteryBox_MysteryId(int mysteryId);
    Page<Review> findByMysteryBox_MysteryId(int mysteryId, Pageable pageable);

    List<Review> findByBuyer_Id(int buyerId);
    Page<Review> findByBuyer_Id(int buyerId, Pageable pageable);

    boolean existsByOrderDetail_OrderDetailId(int orderDetailId);
    boolean existsByMysteryBox_MysteryIdAndBuyer_Id(int mysteryId, int buyerId);

    Long countByShop_Id(int shopId);
    Long countByProduct_ProductId(int productId);

    @Query("SELECT COALESCE(AVG(r.ratingStar), 0.0) FROM Review r WHERE r.shop.id = :shopId")
    Double getAverageRatingByShopId(@Param("shopId") int shopId);

    @Query("SELECT COALESCE(AVG(r.ratingStar), 0.0) FROM Review r WHERE r.product.productId = :productId")
    Double getAverageRatingByProductId(@Param("productId") int productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.shop.id = :shopId AND r.ratingStar = :ratingStar")
    long countByShopIdAndRatingStar(@Param("shopId") int shopId, @Param("ratingStar") BigDecimal ratingStar);
}
