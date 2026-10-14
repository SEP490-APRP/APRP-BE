package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BuildCombo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildComboRepository extends JpaRepository<BuildCombo, Integer>, JpaSpecificationExecutor<BuildCombo> {

    Page<BuildCombo> findByShop_Id(Integer shopId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE BuildCombo c SET c.soldQuantity = c.soldQuantity + :quantity WHERE c.comboId = :comboId AND (c.totalQuantity - c.soldQuantity) >= :quantity")
    int decreaseStockAtomic(@org.springframework.data.repository.query.Param("comboId") Integer comboId, @org.springframework.data.repository.query.Param("quantity") Integer quantity);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE BuildCombo c SET c.soldQuantity = c.soldQuantity - :quantity WHERE c.comboId = :comboId AND c.soldQuantity >= :quantity")
    int increaseStockAtomic(@org.springframework.data.repository.query.Param("comboId") Integer comboId, @org.springframework.data.repository.query.Param("quantity") Integer quantity);
}
