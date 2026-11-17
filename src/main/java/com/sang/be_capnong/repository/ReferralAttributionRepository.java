package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ReferralAttribution;
import com.sang.be_capnong.enums.AttributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralAttributionRepository extends JpaRepository<ReferralAttribution, Long> {

    Optional<ReferralAttribution> findByBuyer_IdAndReferralLink_Id(Integer buyerId, Long referralLinkId);

    List<ReferralAttribution> findByBuyer_IdAndStatus(Integer buyerId, AttributionStatus status);

    @Query("SELECT a FROM ReferralAttribution a WHERE a.buyer.id = :buyerId AND a.status = 'ACTIVE' AND a.expiresAt > :now ORDER BY a.attributedAt DESC")
    List<ReferralAttribution> findActiveAttributionsForBuyer(@Param("buyerId") Integer buyerId, @Param("now") LocalDateTime now);

    @Query("SELECT a FROM ReferralAttribution a JOIN a.referralLink l WHERE a.buyer.id = :buyerId AND a.status = 'ACTIVE' AND a.expiresAt > :now AND ((a.targetType = 'PRODUCT' AND a.targetId = :productId) OR (a.targetType = 'SHOP' AND a.targetId = :shopId)) ORDER BY a.attributedAt DESC")
    List<ReferralAttribution> findMatchingActiveAttributions(
            @Param("buyerId") Integer buyerId,
            @Param("productId") Integer productId,
            @Param("shopId") Integer shopId,
            @Param("now") LocalDateTime now);
}

