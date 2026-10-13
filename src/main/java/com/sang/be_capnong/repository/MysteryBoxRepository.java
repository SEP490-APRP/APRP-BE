package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.MysteryBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MysteryBoxRepository extends JpaRepository<MysteryBox, Integer>, JpaSpecificationExecutor<MysteryBox> {

    Page<MysteryBox> findByShop_Id(Integer shopId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE MysteryBox m SET m.soldQuantity = m.soldQuantity + :quantity WHERE m.mysteryId = :mysteryId AND (m.totalQuantity - m.soldQuantity) >= :quantity")
    int decreaseStockAtomic(@org.springframework.data.repository.query.Param("mysteryId") Integer mysteryId, @org.springframework.data.repository.query.Param("quantity") Integer quantity);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE MysteryBox m SET m.soldQuantity = m.soldQuantity - :quantity WHERE m.mysteryId = :mysteryId AND m.soldQuantity >= :quantity")
    int increaseStockAtomic(@org.springframework.data.repository.query.Param("mysteryId") Integer mysteryId, @org.springframework.data.repository.query.Param("quantity") Integer quantity);
}
