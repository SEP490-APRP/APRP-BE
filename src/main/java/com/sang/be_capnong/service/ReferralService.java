package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.referral.ReferralClaimRequest;
import com.sang.be_capnong.dto.request.referral.ReferralClickTrackRequest;
import com.sang.be_capnong.dto.request.referral.ReferralLinkCreateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.referral.ReferralCommissionResponse;
import com.sang.be_capnong.dto.response.referral.ReferralLinkResponse;
import com.sang.be_capnong.dto.response.referral.ReferralResolveResponse;
import com.sang.be_capnong.dto.response.referral.ReferralStatsResponse;
import org.springframework.data.domain.Pageable;

public interface ReferralService {

    ReferralLinkResponse getOrCreateLink(ReferralLinkCreateRequest request);

    PageResponse<ReferralLinkResponse> getMyLinks(Pageable pageable);

    ReferralResolveResponse resolveLink(String referralCode);

    void trackClick(ReferralClickTrackRequest request, String ipAddress, String userAgent);

    void claimReferral(ReferralClaimRequest request);

    ReferralStatsResponse getDashboard();

    PageResponse<ReferralCommissionResponse> getMyCommissions(Pageable pageable);

    void processOrderReferral(com.sang.be_capnong.entity.Order order, java.util.List<com.sang.be_capnong.entity.OrderDetail> details, java.math.BigDecimal shopVoucherTotal);

    void handleOrderCancelled(int orderId);
}
