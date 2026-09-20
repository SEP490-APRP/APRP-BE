package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.enums.MealType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildComboRepository extends JpaRepository<BuildCombo, Integer> {

    @Query("SELECT b FROM BuildCombo b WHERE " +
            "(:status IS NULL OR LOWER(b.status) = LOWER(:status)) AND " +
            "(:mealType IS NULL OR b.mealType = :mealType) AND " +
            "(:region IS NULL OR LOWER(b.region) LIKE LOWER(CONCAT('%', :region, '%'))) AND " +
            "(:type IS NULL OR LOWER(b.type) = LOWER(:type)) AND " +
            "(:keyword IS NULL OR LOWER(b.comboName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<BuildCombo> findCombos(@Param("status") String status,
                                @Param("mealType") MealType mealType,
                                @Param("region") String region,
                                @Param("type") String type,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    @Query("SELECT b FROM BuildCombo b WHERE b.shop.id = :shopId AND " +
            "(:status IS NULL OR LOWER(b.status) = LOWER(:status)) AND " +
            "(:mealType IS NULL OR b.mealType = :mealType)")
    Page<BuildCombo> findByShopIdAndOptionalFilters(@Param("shopId") Integer shopId,
                                                    @Param("status") String status,
                                                    @Param("mealType") MealType mealType,
                                                    Pageable pageable);

    Page<BuildCombo> findByShop_Id(Integer shopId, Pageable pageable);
}
