package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Payment;
import com.sang.be_capnong.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPayosOrderCode(String payosOrderCode);
    List<Payment> findByOrder_OrderId(int orderId);
    boolean existsByOrder_OrderIdAndStatus(int orderId, PaymentStatus status);
}
