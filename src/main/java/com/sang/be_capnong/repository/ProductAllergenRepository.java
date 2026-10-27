package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ProductAllergen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductAllergenRepository extends JpaRepository<ProductAllergen, Integer> {
    List<ProductAllergen> findByProduct_ProductId(Integer productId);
    List<ProductAllergen> findByProduct_ProductIdIn(Collection<Integer> productIds);
    boolean existsByProduct_ProductIdAndAllergen_Id(Integer productId, Integer allergenId);
    void deleteByProduct_ProductId(Integer productId);
    void deleteByProduct_ProductIdAndAllergen_Id(Integer productId, Integer allergenId);
}
