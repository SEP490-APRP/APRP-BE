package com.sang.be_capnong.dto.request.referral;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralClickTrackRequest {

    @NotBlank(message = "REFERRAL_LINK_NOT_FOUND")
    String referralCode;
}

