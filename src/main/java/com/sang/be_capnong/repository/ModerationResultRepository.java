package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ModerationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModerationResultRepository extends JpaRepository<ModerationResult, Integer> {
    Optional<ModerationResult> findByProduct_ProductId(int productId);
}
