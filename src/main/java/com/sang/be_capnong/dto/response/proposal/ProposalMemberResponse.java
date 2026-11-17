package com.sang.be_capnong.dto.response.proposal;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposalMemberResponse {
    int memberId;
    int shopId;
    String shopName;
    String logoUrl;
    Double ratingAverage;
    /** Chỉ hiện khi cả 2 bên đã tham gia */
    String phone;
    String role;
    String status;
    BigDecimal estimatedKg;
    String message;
    Double distanceKm;
    boolean verified;
    LocalDateTime invitedAt;
    LocalDateTime respondedAt;
    LocalDateTime expiresAt;
}
