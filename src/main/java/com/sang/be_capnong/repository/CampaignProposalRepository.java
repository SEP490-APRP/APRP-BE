package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.CampaignProposal;
import com.sang.be_capnong.enums.CampaignProposalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CampaignProposalRepository extends JpaRepository<CampaignProposal, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CampaignProposal p where p.proposalId = :id")
    Optional<CampaignProposal> findByIdForUpdate(@Param("id") int id);

    boolean existsByCode(String code);

    List<CampaignProposal> findByInitiator_IdOrderByCreatedAtDesc(int shopId);

    boolean existsByInitiator_IdAndStatusIn(int shopId, Collection<CampaignProposalStatus> statuses);

    List<CampaignProposal> findByInitiator_IdAndStatusAndReviewedAtAfter(int shopId, CampaignProposalStatus status, LocalDateTime after);

    List<CampaignProposal> findByStatusInOrderBySubmittedAtAsc(Collection<CampaignProposalStatus> statuses);

    List<CampaignProposal> findAllByOrderByCreatedAtDesc();

    List<CampaignProposal> findByCampaign_CampaignId(int campaignId);

    /** Danh mục shop đang bán: [shopId, categoryId, categoryName] */
    @Query("select p.shop.id, c.categoryId, c.name from Product p join p.category c where p.shop.id in :shopIds "
            + "group by p.shop.id, c.categoryId, c.name")
    List<Object[]> findShopCategories(@Param("shopIds") Collection<Integer> shopIds);
}
