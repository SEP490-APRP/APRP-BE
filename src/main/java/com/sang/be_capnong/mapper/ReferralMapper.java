package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.response.referral.ReferralCommissionResponse;
import com.sang.be_capnong.dto.response.referral.ReferralLinkResponse;
import com.sang.be_capnong.entity.ReferralCommission;
import com.sang.be_capnong.entity.ReferralLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReferralMapper {

    @Mapping(target = "shareUrl", ignore = true)
    @Mapping(target = "targetName", ignore = true)
    @Mapping(target = "targetImageUrl", ignore = true)
    ReferralLinkResponse toReferralLinkResponse(ReferralLink referralLink);

    List<ReferralLinkResponse> toReferralLinkResponseList(List<ReferralLink> referralLinks);

    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderDetailId", source = "orderDetail.orderDetailId")
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productImageUrl", ignore = true)
    ReferralCommissionResponse toReferralCommissionResponse(ReferralCommission referralCommission);

    List<ReferralCommissionResponse> toReferralCommissionResponseList(List<ReferralCommission> referralCommissions);
}

