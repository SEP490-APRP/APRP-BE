package com.sang.be_capnong.dto.response.hub;

import com.sang.be_capnong.dto.request.hub.HubTripStop;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubTripResponse {
    int tripId;
    int campaignId;
    String campaignTitle;
    int tripNo;
    LocalDate pickupDate;
    double totalKg;
    /** OPEN | ACCEPTED | IN_TRANSIT | DELIVERED | CANCELLED */
    String status;
    boolean confirmed;
    List<HubTripStop> stops;
    Integer driverId;
    Integer vehicleKg;
    /** Thùng phải nhận lên xe và số đã nhận */
    List<HubTripBox> boxes;
    int boxTotal;
    int boxLoaded;
    /** Tài xế đã bấm không nhận chuyến */
    List<Integer> declinedDriverIds;
    /** Tài xế đang xem đã bấm không nhận (chỉ có ở danh sách chuyến đang chờ) */
    boolean declined;
    String driverName;
    String driverPhone;
    String vehicleNumber;
    String collectionSiteName;
    String collectionSiteAddress;
    Double collectionSiteLatitude;
    Double collectionSiteLongitude;
    /** Quãng đường điểm tập kết -> hub (km), cước gộp, số tài xế nhận sau phí dịch vụ */
    Double routeKm;
    double feeAmount;
    double netAmount;
    /** Tỉ lệ phí dịch vụ (0-1) */
    double serviceFeeRate;
    LocalDateTime paidAt;
    LocalDateTime acceptedAt;
    LocalDateTime startedAt;
    String loadPhotoUrl;
    LocalDateTime deliveredAt;
}
