package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.CheckoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckoutBatchRepository extends JpaRepository<CheckoutBatch, Integer> {
    Optional<CheckoutBatch> findByPayosOrderCode(String payosOrderCode);
}
