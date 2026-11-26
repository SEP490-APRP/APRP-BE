package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Admin kêu gọi xe (shipper) chở hàng chiến dịch từ điểm tập kết ra các hub */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperCallRequest {
    /** Ngày cần xe, vd "2026-11-28" */
    String pickupDate;
    /** Số chuyến / số xe hệ thống ước tính cần */
    Integer vehiclesNeeded;
    /** Tổng kg cần giao */
    Double totalKg;
    /** Nội dung chi tiết (các hub, số chuyến từng nơi) */
    String message;
    /** Kêu gọi riêng cho một hub (mỗi hub gọi một lượt); bỏ trống = gọi chung */
    String hubName;
    /** Tổng số xe của cả kế hoạch, để ghi nhận ở chiến dịch khi gọi theo từng hub */
    Integer totalVehicles;
    /** Chỉ gửi cho các shipper này; bỏ trống = mọi shipper đang hoạt động */
    List<Integer> shipperIds;
    /** Sức chở tối thiểu (kg) của xe để nhận kêu gọi = chuyến nhỏ nhất trong kế hoạch; xe nhỏ hơn không được gửi */
    Integer minVehicleKg;
    /** true = báo phân công xe cho tài xế đã chọn (không tính là lần kêu gọi chung) */
    Boolean assignment;
    /** Số kg của xe được phân công (khi assignment = true) */
    Double vehicleKg;
}
