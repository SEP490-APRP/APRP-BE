package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/** Bảng chia đơn giao của một chiến dịch: đơn giao tận nơi gom theo phường, xếp theo độ gấp */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatchBoardResponse {
    int campaignId;
    String campaignStatus;
    /** Độ bền ngắn nhất của các lô nhà vườn (DAYS_2_3 / DAYS_3_4 / DAYS_5_6) */
    String shelfLifeTier;
    long urgentCount;
    long highCount;
    long normalCount;
    /** Đơn đã đóng gói / giao thất bại chờ chia */
    long waitingCount;
    /** Đơn đã ra hub (có hoặc chưa có shipper) */
    long dispatchedCount;
    List<Zone> zones;
    List<ShipperOption> shippers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Zone {
        String zone;
        int orderCount;
        BigDecimal totalKg;
        /** Tổng tiền shipper phải thu khi giao */
        BigDecimal codTotal;
        /** URGENT | HIGH | NORMAL: mức gấp nhất trong khu */
        String topPriority;
        List<PreOrderResponse> orders;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ShipperOption {
        int id;
        String name;
        String phone;
        String vehicleNumber;
        /** Loại xe và sức chở (kg): lọc tài xế đủ tải cho chuyến chở hàng ra hub */
        String vehicleType;
        int vehicleCapacityKg;
        boolean online;
        /** Số đơn chiến dịch đang cầm (chưa giao xong) */
        long activePreOrders;
        /** Vị trí gần nhất shipper báo về (null nếu chưa có) và thời điểm báo */
        Double latitude;
        Double longitude;
        java.time.LocalDateTime locationUpdatedAt;
    }
}
