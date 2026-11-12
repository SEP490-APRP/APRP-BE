package com.sang.be_capnong.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipper_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    Double latitude;
    Double longitude;
    LocalDateTime updatedAt;
    String deliveryPhase;

    /** Shipper đang bật nhận đơn. Tắt online không xoá dòng để vẫn theo dõi được đơn đang giao */
    Boolean online;

    /** Lần cuối app báo còn hoạt động (heartbeat). Quá hạn thì scheduler tự chuyển offline */
    LocalDateTime lastSeenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    User shipper;
}
