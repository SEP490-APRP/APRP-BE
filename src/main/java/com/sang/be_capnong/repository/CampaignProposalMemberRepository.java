package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.CampaignProposalMember;
import com.sang.be_capnong.enums.ProposalMemberRole;
import com.sang.be_capnong.enums.ProposalMemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CampaignProposalMemberRepository extends JpaRepository<CampaignProposalMember, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from CampaignProposalMember m where m.memberId = :id")
    Optional<CampaignProposalMember> findByIdForUpdate(@Param("id") int id);

    List<CampaignProposalMember> findByProposal_ProposalIdOrderByMemberIdAsc(int proposalId);

    Optional<CampaignProposalMember> findByProposal_ProposalIdAndShop_Id(int proposalId, int shopId);

    List<CampaignProposalMember> findByShop_IdAndRoleOrderByInvitedAtDesc(int shopId, ProposalMemberRole role);

    long countByProposal_ProposalIdAndRoleAndStatusNot(int proposalId, ProposalMemberRole role, ProposalMemberStatus status);

    /** Số lời mời một shop khởi xướng đã gửi kể từ mốc thời gian (chống spam) */
    @Query("select count(m) from CampaignProposalMember m where m.proposal.initiator.id = :shopId "
            + "and m.role = com.sang.be_capnong.enums.ProposalMemberRole.INVITED and m.invitedAt >= :since")
    long countInvitesSentSince(@Param("shopId") int shopId, @Param("since") LocalDateTime since);

    List<CampaignProposalMember> findTop500ByStatusAndExpiresAtBefore(ProposalMemberStatus status, LocalDateTime before);

    List<CampaignProposalMember> findByProposal_ProposalIdInAndStatusIn(Collection<Integer> proposalIds, Collection<ProposalMemberStatus> statuses);
}
