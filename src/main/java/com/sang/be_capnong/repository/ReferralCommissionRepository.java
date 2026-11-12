package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ReferralCommission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralCommissionRepository extends JpaRepository<ReferralCommission, Long> {

    List<ReferralCommission> findByOrder_OrderId(Integer orderId);

    Optional<ReferralCommission> findByOrderDetail_OrderDetailId(Integer orderDetailId);

    Page<ReferralCommission> findByReferrer_Id(Integer referrerId, Pageable pageable);

    @Modifying
    @Query("UPDATE ReferralCommission c SET c.status = 'COMPLETED', c.baseAmount = :baseAmount, c.commissionAmount = :commissionAmount, c.settledAt = :settledAt, c.updatedAt = :now WHERE c.id = :id AND c.status = 'PENDING'")
    int markCompletedAtomic(
            @Param("id") Long id,
            @Param("baseAmount") BigDecimal baseAmount,
            @Param("commissionAmount") BigDecimal commissionAmount,
            @Param("settledAt") LocalDateTime settledAt,
            @Param("now") LocalDateTime now);

    @Query("SELECT c FROM ReferralCommission c JOIN c.order o WHERE c.status = 'PENDING' AND o.settledAt IS NOT NULL")
    List<ReferralCommission> findPendingCommissionsOfSettledOrders();

    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM ReferralCommission c WHERE c.referrer.id = :referrerId AND c.status = 'COMPLETED'")
    BigDecimal sumEarnedByReferrer(@Param("referrerId") Integer referrerId);

    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM ReferralCommission c WHERE c.referrer.id = :referrerId AND c.status = 'PENDING'")
    BigDecimal sumPendingByReferrer(@Param("referrerId") Integer referrerId);

    @Query("SELECT COUNT(DISTINCT c.order.orderId) FROM ReferralCommission c WHERE c.referrer.id = :referrerId AND c.status = 'COMPLETED'")
    long countOrdersByReferrer(@Param("referrerId") Integer referrerId);
}

