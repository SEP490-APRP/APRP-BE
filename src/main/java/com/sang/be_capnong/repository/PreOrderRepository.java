package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.PreOrder;
import com.sang.be_capnong.enums.PreOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PreOrderRepository extends JpaRepository<PreOrder, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PreOrder p where p.preOrderId = :id")
    Optional<PreOrder> findByIdForUpdate(@Param("id") int id);

    List<PreOrder> findByCampaign_CampaignIdOrderByCreatedAtAsc(int campaignId);

    List<PreOrder> findByCampaign_CampaignIdAndStatusIn(int campaignId, Collection<PreOrderStatus> statuses);

    Page<PreOrder> findByBuyer_IdOrderByCreatedAtDesc(int buyerId, Pageable pageable);

    List<PreOrder> findTop200ByStatusAndCreatedAtBefore(PreOrderStatus status, LocalDateTime before);

    List<PreOrder> findByStatusAndShipperIsNullOrderByReadyAtAsc(PreOrderStatus status);

    List<PreOrder> findByShipper_IdOrderByUpdatedAtDesc(int shipperId);

    /** Số kg đang giữ chỗ (chờ cọc) để không cho đặt vượt mục tiêu */
    @Query("select coalesce(sum(p.totalKg), 0) from PreOrder p where p.campaign.campaignId = :campaignId "
            + "and p.status = com.sang.be_capnong.enums.PreOrderStatus.PENDING")
    BigDecimal sumPendingKg(@Param("campaignId") int campaignId);

    /**
     * Như sumPendingKg nhưng là locking read (FOR UPDATE): luôn đọc bản đã commit mới nhất, không dùng snapshot
     * REPEATABLE READ của giao dịch. Dùng khi tính sản lượng còn lại lúc đặt đơn để 2 người không cùng giành được thùng cuối.
     */
    @Query(value = "SELECT COALESCE(SUM(total_kg), 0) FROM pre_orders WHERE campaign_id = :campaignId AND status = 'PENDING' FOR UPDATE",
            nativeQuery = true)
    BigDecimal sumPendingKgForUpdate(@Param("campaignId") int campaignId);

    boolean existsByCode(String code);

    @Query("select coalesce(sum(p.totalKg), 0) from PreOrder p where p.campaign.campaignId = :campaignId and p.status in :statuses")
    BigDecimal sumKgByStatuses(@Param("campaignId") int campaignId, @Param("statuses") Collection<PreOrderStatus> statuses);

    /** Đơn đã cọc chờ đóng gói, ai cọc trước được giữ hàng trước */
    List<PreOrder> findByCampaign_CampaignIdAndStatusOrderByDepositPaidAtAscPreOrderIdAsc(int campaignId, PreOrderStatus status);

    long countByCampaign_CampaignIdAndStatusIn(int campaignId, Collection<PreOrderStatus> statuses);
}
