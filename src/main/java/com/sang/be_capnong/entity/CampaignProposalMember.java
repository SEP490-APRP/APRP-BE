package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.ProposalMemberRole;
import com.sang.be_capnong.enums.ProposalMemberStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Shop trong đơn đề xuất: shop khởi xướng hoặc shop lân cận được mời. Số kg chỉ là dự kiến, không ràng buộc */
@Entity
@Table(name = "campaign_proposal_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_proposal_shop", columnNames = {"proposal_id", "shop_id"}),
        indexes = {
                @Index(name = "idx_cpm_shop_status", columnList = "shop_id, status"),
                @Index(name = "idx_cpm_status_expires", columnList = "status, expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignProposalMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    CampaignProposal proposal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    ProposalMemberRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    ProposalMemberStatus status;

    @Column(precision = 15, scale = 2)
    BigDecimal estimatedKg;

    /** Lời nhắn khi mời */
    @Column(length = 300)
    String message;

    /** Khoảng cách từ điểm tập kết tới shop lúc mời (km) */
    Double distanceKm;

    LocalDateTime invitedAt;
    LocalDateTime respondedAt;
    LocalDateTime expiresAt;
}
