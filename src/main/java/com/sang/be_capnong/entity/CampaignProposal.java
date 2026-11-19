package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.CampaignProposalStatus;
import com.sang.be_capnong.enums.ShelfLifeTier;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Shop đề xuất mở chiến dịch giải cứu; admin duyệt thì tạo chiến dịch nháp (có thể gộp nhiều đơn gần nhau) */
@Entity
@Table(name = "campaign_proposals", indexes = {
        @Index(name = "idx_cp_status", columnList = "status, submitted_at"),
        @Index(name = "idx_cp_initiator", columnList = "initiator_shop_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int proposalId;

    @Column(nullable = false, unique = true, length = 32)
    String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_shop_id", nullable = false)
    User initiator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    @Column(length = 200)
    String productName;

    /** URL ảnh, cách nhau bởi dấu phẩy (1–5 ảnh) */
    @Column(columnDefinition = "TEXT")
    String photoUrls;

    /** Lý do cần giải cứu: giá rớt, thương lái bỏ, thời tiết... */
    @Column(columnDefinition = "TEXT")
    String story;

    LocalDate harvestFrom;
    LocalDate harvestTo;

    /** Shop đề xuất ngày mở bán và ngày kết thúc (chốt) chiến dịch; admin dùng làm gợi ý khi tạo chiến dịch */
    LocalDate proposedOpenDate;
    LocalDate proposedCloseDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    ShelfLifeTier shelfLifeTier;

    @Column(precision = 15, scale = 2)
    BigDecimal desiredPricePerKg;

    @Column(precision = 15, scale = 2)
    BigDecimal marketPricePerKg;

    @Column(length = 200)
    String siteName;

    @Column(length = 500)
    String siteAddress;

    Double siteLatitude;
    Double siteLongitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    CampaignProposalStatus status;

    @Column(length = 500)
    String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;

    LocalDateTime reviewedAt;

    /** Chiến dịch được tạo khi duyệt; nhiều đơn gộp lại thì cùng trỏ về 1 chiến dịch */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    Campaign campaign;

    LocalDateTime submittedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "proposal")
    @OrderBy("memberId ASC")
    @Builder.Default
    List<CampaignProposalMember> members = new ArrayList<>();
}
