package com.sang.be_capnong.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Điểm trung chuyển (hub) gần khu dân cư. Xe của CapNong chở thùng đã đóng gói từ điểm tập kết tới đây,
 * shipper tới hub lấy hàng rồi giao chặng cuối tới người mua.
 */
@Entity
@Table(name = "delivery_hubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryHub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hub_id")
    int hubId;

    @Column(nullable = false, length = 150)
    String name;

    @Column(nullable = false, length = 500)
    String address;

    @Column(nullable = false)
    Double latitude;

    @Column(nullable = false)
    Double longitude;

    /** Số điện thoại người trực hub */
    @Column(length = 20)
    String phone;

    /** Giờ nhận / giao hàng, vd "6:00 – 20:00" */
    @Column(length = 100)
    String openHours;

    @Column(length = 500)
    String note;

    /** false = ngưng dùng (không gán đơn mới), vẫn giữ để đơn cũ tra được */
    @Column(nullable = false)
    @Builder.Default
    boolean active = true;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
