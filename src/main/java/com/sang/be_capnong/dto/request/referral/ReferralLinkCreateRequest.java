package com.sang.be_capnong.dto.request.referral;

import com.sang.be_capnong.enums.ReferralTargetType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralLinkCreateRequest {

    @NotNull(message = "REFERRAL_INVALID_TARGET")
    ReferralTargetType targetType;

    @NotNull(message = "REFERRAL_INVALID_TARGET")
    Integer targetId;
}

