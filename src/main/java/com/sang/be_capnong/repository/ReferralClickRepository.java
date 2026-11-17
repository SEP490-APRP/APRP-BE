package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ReferralClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReferralClickRepository extends JpaRepository<ReferralClick, Long> {

    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime after);

    long countByReferralLink_IdAndIpAddressAndCreatedAtAfter(Long linkId, String ipAddress, LocalDateTime after);
}

