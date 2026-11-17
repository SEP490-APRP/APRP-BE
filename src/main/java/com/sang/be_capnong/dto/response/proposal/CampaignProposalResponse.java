package com.sang.be_capnong.dto.response.proposal;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignProposalResponse {
    int proposalId;
    String code;
    String status;
    int initiatorShopId;
    String initiatorShopName;
    String initiatorLogoUrl;
    Double initiatorRating;
    boolean initiatorVerified;
    Integer categoryId;
    String categoryName;
    String productName;
    List<String> photoUrls;
    String story;
    LocalDate harvestFrom;
    LocalDate harvestTo;
    String shelfLifeTier;
    BigDecimal desiredPricePerKg;
    BigDecimal marketPricePerKg;
    String siteName;
    String siteAddress;
    Double siteLatitude;
    Double siteLongitude;
    /** Tổng kg dự kiến của shop khởi xướng + các shop đã nhận lời */
    BigDecimal totalEstimatedKg;
    int acceptedShopCount;
    int pendingInviteCount;
    BigDecimal minSubmitKg;
    String adminNote;
    LocalDateTime reviewedAt;
    Integer campaignId;
    String campaignCode;
    String campaignStatus;
    LocalDateTime submittedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    /** Với shop được mời: lời mời của chính mình */
    ProposalMemberResponse myInvite;
    List<ProposalMemberResponse> members;
    /** Admin: các chiến dịch trước đây shop khởi xướng đã tham gia (số lô đã đăng ký) */
    Long initiatorPastRegistrations;
}
