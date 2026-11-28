package com.sang.be_capnong.service.campaign;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** Gom thông số tính cước (xe ra hub + giao chặng cuối) để FE ước tính chi phí chuyến trước khi lưu */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HubFeeConfigService {

    HubTripFeeCalculator hubTripFee;
    CampaignShippingCalculator shipping;

    /** Phí dịch vụ trên cước xe ra hub (mặc định 0) */
    @Value("${capnong.campaign.hubtrip.fee-rate:0}")
    @lombok.experimental.NonFinal
    double hubServiceFeeRate = 0;

    @Value("${capnong.wallet.shipper-fee-rate:0.03}")
    @lombok.experimental.NonFinal
    double serviceFeeRate = 0.03;

    /** Hoa hồng sàn giữ trên tiền hàng chiến dịch: để màn dự toán tính tiền thu về */
    @Value("${capnong.campaign.farmer-fee-rate:0.30}")
    @lombok.experimental.NonFinal
    double farmerFeeRate = 0.30;

    public Map<String, Object> config() {
        Map<String, Object> m = new HashMap<>(hubTripFee.config());
        // Cước giao chặng cuối theo khoảng cách hub -> người mua, bảng phí theo khoảng cách
        m.put("lastMile", Map.of(
                "under5", shipping.baseFee(1).longValue(),
                "from5to10", shipping.baseFee(7).longValue(),
                "from10to20", shipping.baseFee(15).longValue(),
                "perKmOver20", shipping.baseFee(21).subtract(shipping.baseFee(20)).longValue()));
        m.put("serviceFeeRate", serviceFeeRate);
        m.put("hubServiceFeeRate", hubServiceFeeRate);
        m.put("farmerFeeRate", farmerFeeRate);
        return m;
    }
}
