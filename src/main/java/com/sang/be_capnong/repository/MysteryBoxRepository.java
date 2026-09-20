package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.MysteryBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface MysteryBoxRepository extends JpaRepository<MysteryBox, Integer> {

    @Query("SELECT m FROM MysteryBox m WHERE " +
            "(:shopId IS NULL OR m.shop.id = :shopId) AND " +
            "(:boxType IS NULL OR LOWER(m.boxType) = LOWER(:boxType)) AND " +
            "(:isActive IS NULL OR m.isActive = :isActive) AND " +
            "(:minPrice IS NULL OR m.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR m.price <= :maxPrice)")
    Page<MysteryBox> findMysteryBoxes(@Param("shopId") Integer shopId,
                                      @Param("boxType") String boxType,
                                      @Param("isActive") Boolean isActive,
                                      @Param("minPrice") BigDecimal minPrice,
                                      @Param("maxPrice") BigDecimal maxPrice,
                                      Pageable pageable);

    Page<MysteryBox> findByShop_Id(Integer shopId, Pageable pageable);
}
