package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.PreOrderDamageClaim;
import com.sang.be_capnong.enums.DamageClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PreOrderDamageClaimRepository extends JpaRepository<PreOrderDamageClaim, Integer> {

    List<PreOrderDamageClaim> findByPreOrder_PreOrderIdOrderByCreatedAtDesc(int preOrderId);

    List<PreOrderDamageClaim> findByPreOrder_Campaign_CampaignIdOrderByCreatedAtDesc(int campaignId);

    boolean existsByPreOrder_PreOrderIdAndStatus(int preOrderId, DamageClaimStatus status);

    long countByPreOrder_Campaign_CampaignIdAndStatus(int campaignId, DamageClaimStatus status);

    /** Kg đã báo (chờ duyệt) hoặc đã được duyệt của một đơn */
    @Query("select coalesce(sum(case when c.status = com.sang.be_capnong.enums.DamageClaimStatus.APPROVED then c.approvedKg else c.claimedKg end), 0) "
            + "from PreOrderDamageClaim c where c.preOrder.preOrderId = :preOrderId and c.status in :statuses")
    BigDecimal sumKg(@Param("preOrderId") int preOrderId, @Param("statuses") Collection<DamageClaimStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PreOrderDamageClaim c where c.claimId = :id")
    Optional<PreOrderDamageClaim> findByIdForUpdate(@Param("id") int id);
}
