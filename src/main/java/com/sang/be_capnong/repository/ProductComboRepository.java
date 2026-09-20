package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ProductCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductComboRepository extends JpaRepository<ProductCombo, Integer> {

    List<ProductCombo> findByBuildComboComboId(int comboId);

    void deleteByBuildComboComboId(int comboId);
}
