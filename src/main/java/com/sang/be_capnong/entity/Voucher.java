package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.VoucherType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    int voucherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @Column(length = 20, nullable = false)
    String voucherCode;

    BigDecimal discountValue;
    BigDecimal maxDiscount;
    BigDecimal minOrderValue;

    Integer usageLimit;
    LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    VoucherType voucherType;

    Integer claimLimit;
    Integer claimedCount;
    Integer usedCount;
    Integer perUserLimit;

    Boolean active;
    LocalDateTime createAt;

    @Column(length = 100)
    String autoCreatedReason;

    Integer autoCreatedFromOrderId;

    @OneToMany(mappedBy = "voucher")
    @JsonIgnore
    List<VoucherUser> voucherUsers;

    @OneToMany(mappedBy = "voucher")
    @JsonIgnore
    List<VoucherUsage> voucherUsages;
}
