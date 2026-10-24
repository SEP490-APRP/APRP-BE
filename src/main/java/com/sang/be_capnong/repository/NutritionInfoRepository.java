package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.NutritionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionInfoRepository extends JpaRepository<NutritionInfo, Integer> {
    Optional<NutritionInfo> findByProduct_ProductId(Integer productId);
    List<NutritionInfo> findByProduct_ProductIdIn(Collection<Integer> productIds);
    void deleteByProduct_ProductId(Integer productId);
}
