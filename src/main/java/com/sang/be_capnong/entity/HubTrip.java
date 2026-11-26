package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.HubTripStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Một chuyến xe chở hàng của chiến dịch từ điểm tập kết ra các hub. Admin lập kế hoạch xe (kêu gọi trước, chốt sau khi cân),
 * tài xế nhận chuyến, lấy hàng tại điểm tập kết rồi giao tới hub. Shipper tại hub nhận đơn giao chặng cuối như luồng hub.
 */
@Entity
@Table(name = "hub_trips", indexes = {
        @Index(name = "idx_hub_trip_campaign_status", columnList = "campaign_id, status"),
        @Index(name = "idx_hub_trip_driver", columnList = "driver_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    int tripId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    Campaign campaign;

    /** Số thứ tự xe trong kế hoạch của chiến dịch (Xe 1, Xe 2...) */
    @Column(nullable = false)
    int tripNo;

    /** Ngày cần xe = ngày kết thúc mở điểm tập kết */
    LocalDate pickupDate;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal totalKg;

    /** Lộ trình dạng JSON: danh sách điểm dừng [{hubId, hubName, hubAddress, kg, orderCount}] theo thứ tự ghé */
    @Column(columnDefinition = "TEXT")
    String stopsJson;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    HubTripStatus status;

    /** true = đã chốt kế hoạch sau khi cân hàng (tài xế mới được bấm lấy hàng); false = dự kiến */
    @Column(nullable = false)
    boolean confirmed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    User driver;

    /** Thùng tài xế đã nhận lên xe, dạng "mãĐơn-sốThùng" cách nhau bằng dấu phẩy */
    @Column(columnDefinition = "TEXT")
    String loadedBoxes;

    /** Cỡ xe (kg) admin đã chốt khi gọi chuyến này */
    Integer vehicleKg;

    /** Mã các tài xế đã bấm không nhận chuyến này, cách nhau bằng dấu phẩy */
    @Column(columnDefinition = "TEXT")
    String declinedDriverIds;

    /** Quãng đường điểm tập kết -> các hub (km, ước tính) */
    Double routeKm;

    /** Cước gộp của chuyến (trước phí dịch vụ), tính theo HubTripFeeCalculator */
    @Column(precision = 15, scale = 2)
    BigDecimal feeAmount;

    /** Số tài xế đã nhận (sau phí dịch vụ) và thời điểm trả; null = chưa trả */
    @Column(precision = 15, scale = 2)
    BigDecimal payoutAmount;
    LocalDateTime paidAt;

    LocalDateTime acceptedAt;
    LocalDateTime startedAt;
    /** Ảnh tài xế chụp hàng đã bốc lên xe tại điểm tập kết */
    @Column(length = 500)
    String loadPhotoUrl;
    LocalDateTime deliveredAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
