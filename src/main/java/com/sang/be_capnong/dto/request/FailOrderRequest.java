package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.DeliveryFailReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Shipper báo giao thất bại: lý do, số cuộc gọi, ảnh bằng chứng */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FailOrderRequest {
    DeliveryFailReason reason;
    String note;
    Integer callAttempts;
    List<String> proofImageUrls;
}
