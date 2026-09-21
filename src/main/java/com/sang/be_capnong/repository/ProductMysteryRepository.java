package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ProductMystery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMysteryRepository extends JpaRepository<ProductMystery, Integer> {
    List<ProductMystery> findByMysteryBox_MysteryId(int mysteryId);
    void deleteByMysteryBox_MysteryId(int mysteryId);
    List<ProductMystery> findByProduct_ProductId(int productId);
}
