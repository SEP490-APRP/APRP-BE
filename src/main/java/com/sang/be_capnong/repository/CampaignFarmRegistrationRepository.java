package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.CampaignFarmRegistration;
import com.sang.be_capnong.enums.FarmRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignFarmRegistrationRepository extends JpaRepository<CampaignFarmRegistration, Integer> {

    List<CampaignFarmRegistration> findByCampaign_CampaignIdOrderByCreatedAtAsc(int campaignId);

    List<CampaignFarmRegistration> findByCampaign_CampaignIdAndStatusOrderByCreatedAtAsc(int campaignId, FarmRegistrationStatus status);

    Optional<CampaignFarmRegistration> findByCampaign_CampaignIdAndShop_Id(int campaignId, int shopId);

    List<CampaignFarmRegistration> findByShop_IdOrderByCreatedAtDesc(int shopId);

    @Query("select coalesce(sum(r.surplusVolumeKg), 0) from CampaignFarmRegistration r "
            + "where r.campaign.campaignId = :campaignId and r.status = com.sang.be_capnong.enums.FarmRegistrationStatus.APPROVED")
    BigDecimal sumApprovedSurplus(@Param("campaignId") int campaignId);

    /** Locking read của sumApprovedSurplus (đọc bản mới nhất), dùng khi tính sản lượng còn lại lúc đặt đơn */
    @Query(value = "SELECT COALESCE(SUM(surplus_volume_kg), 0) FROM campaign_farm_registrations "
            + "WHERE campaign_id = :campaignId AND status = 'APPROVED' FOR UPDATE", nativeQuery = true)
    BigDecimal sumApprovedSurplusForUpdate(@Param("campaignId") int campaignId);

    long countByCampaign_CampaignIdAndStatus(int campaignId, FarmRegistrationStatus status);

    /** Số nhà vườn (shop khác nhau) theo trạng thái: một shop góp nhiều lô vẫn chỉ tính 1 */
    @Query("select count(distinct r.shop.id) from CampaignFarmRegistration r where r.campaign.campaignId = :campaignId and r.status = :status")
    long countDistinctShopsByCampaignAndStatus(@Param("campaignId") int campaignId, @Param("status") FarmRegistrationStatus status);
}
