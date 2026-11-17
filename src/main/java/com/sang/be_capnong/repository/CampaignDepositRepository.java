package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.CampaignDeposit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignDepositRepository extends JpaRepository<CampaignDeposit, Integer> {

    Optional<CampaignDeposit> findByPayosOrderCode(String payosOrderCode);

    Optional<CampaignDeposit> findByPreOrder_PreOrderId(int preOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CampaignDeposit d where d.preOrder.preOrderId = :preOrderId")
    Optional<CampaignDeposit> findByPreOrderIdForUpdate(@Param("preOrderId") int preOrderId);

    boolean existsByPayosOrderCode(String payosOrderCode);
}
