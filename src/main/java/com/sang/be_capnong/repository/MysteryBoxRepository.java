package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.MysteryBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MysteryBoxRepository extends JpaRepository<MysteryBox, Integer>, JpaSpecificationExecutor<MysteryBox> {

    @EntityGraph(attributePaths = {"shop"})
    Page<MysteryBox> findByShop_Id(Integer shopId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"shop"})
    Page<MysteryBox> findAll(Specification<MysteryBox> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"shop"})
    Optional<MysteryBox> findById(Integer id);

    @Modifying
    @Query("UPDATE MysteryBox m SET m.soldQuantity = m.soldQuantity + :quantity WHERE m.mysteryId = :mysteryId AND (m.totalQuantity - m.soldQuantity) >= :quantity")
    int decreaseStockAtomic(@Param("mysteryId") Integer mysteryId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE MysteryBox m SET m.soldQuantity = m.soldQuantity - :quantity WHERE m.mysteryId = :mysteryId AND m.soldQuantity >= :quantity")
    int increaseStockAtomic(@Param("mysteryId") Integer mysteryId, @Param("quantity") Integer quantity);
}
