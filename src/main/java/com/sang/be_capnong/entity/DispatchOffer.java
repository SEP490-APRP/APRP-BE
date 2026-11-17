package com.sang.be_capnong.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/** Một lần hệ thống mời Shipper nhận đơn READY (mỗi cặp đơn - shipper chỉ mời 1 lần) */
@Entity
@Table(name = "dispatch_offers",
        uniqueConstraints = @UniqueConstraint(name = "uk_dispatch_offer_order_shipper", columnNames = {"order_id", "shipper_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatchOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    User shipper;

    /** Đợt quét (1, 2, 3...) */
    Integer wave;

    /** Khoảng cách shipper tới nhà vườn lúc mời (km) */
    Double distanceKm;

    LocalDateTime createdAt;
}
