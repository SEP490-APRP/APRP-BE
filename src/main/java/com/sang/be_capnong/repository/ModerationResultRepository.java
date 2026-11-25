package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ModerationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModerationResultRepository extends JpaRepository<ModerationResult, Integer> {

    interface ProductVectorProjection {
        Integer getProductId();
        String getRawResponse();
    }

    Optional<ModerationResult> findByProduct_ProductId(int productId);

    @Query("SELECT p.productId AS productId, mr.rawResponse AS rawResponse " +
           "FROM ModerationResult mr JOIN mr.product p " +
           "WHERE p.status IN ('APPROVED', 'ACTIVE') AND mr.rawResponse IS NOT NULL")
    List<ProductVectorProjection> findAllActiveProductVectorProjections();
}
