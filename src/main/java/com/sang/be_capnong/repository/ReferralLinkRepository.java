package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ReferralLink;
import com.sang.be_capnong.enums.ReferralTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

    Optional<ReferralLink> findByReferralCode(String referralCode);

    Optional<ReferralLink> findByReferrer_IdAndTargetTypeAndTargetId(
            Integer referrerId, ReferralTargetType targetType, Integer targetId);

    Page<ReferralLink> findByReferrer_Id(Integer referrerId, Pageable pageable);

    boolean existsByReferralCode(String referralCode);

    @Modifying
    @Query("UPDATE ReferralLink r SET r.clicksCount = r.clicksCount + 1 WHERE r.id = :id")
    int incrementClicksCount(@Param("id") Long id);
}

