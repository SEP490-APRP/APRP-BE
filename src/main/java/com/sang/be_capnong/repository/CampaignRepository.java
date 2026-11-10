package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Campaign;
import com.sang.be_capnong.enums.CampaignStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Integer> {

    /** Khoá dòng chiến dịch khi cộng sản lượng đã đặt / đổi trạng thái để không vượt mục tiêu */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campaign c where c.campaignId = :id")
    Optional<Campaign> findByIdForUpdate(@Param("id") int id);

    Page<Campaign> findByStatusInOrderByCreatedAtDesc(Collection<CampaignStatus> statuses, Pageable pageable);

    Page<Campaign> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Chiến dịch "sắp mở": còn DRAFT nhưng đã đăng lời kêu gọi nhà vườn */
    Page<Campaign> findByStatusAndCallPostedAtIsNotNullOrderByCreatedAtDesc(CampaignStatus status, Pageable pageable);

    /** Lời kêu gọi đang mở cho nhà vườn: DRAFT đã đăng lời kêu gọi, hoặc ACTIVE */
    @Query("select c from Campaign c where (c.status = com.sang.be_capnong.enums.CampaignStatus.DRAFT and c.callPostedAt is not null) "
            + "or c.status = com.sang.be_capnong.enums.CampaignStatus.ACTIVE order by c.cutoffAt asc")
    List<Campaign> findOpenCalls();

    List<Campaign> findTop100ByStatusAndCutoffAtBefore(CampaignStatus status, LocalDateTime before);

    boolean existsByCode(String code);
}
