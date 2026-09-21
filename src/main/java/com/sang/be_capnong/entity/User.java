package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.DietPreference;
import com.sang.be_capnong.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(unique = true)
    String email;

    @Column(length = 255)
    String password;

    @Column(columnDefinition = "TEXT")
    String fullName;

    @Column(length = 50)
    String phoneNumber;

    String address;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    UserStatus status;

    // ===== SHOP FIELDS =====
    String shopName;

    @Builder.Default
    Boolean shopActive = true;

    String bankAccount;
    String bankAccountHolder;
    String bankName;
    Double ratingAverage;

    @Column(length = 500)
    String description;

    String logoUrl;
    String achievement;
    String license;
    String licenseImageUrl;

    // ===== SHIPPER FIELDS =====
    String vehicleNumber;
    String vehicleDocImageUrl;

    // ===== BUYER FIELDS =====
    @Enumerated(EnumType.STRING)
    DietPreference dietPreference;

    @Builder.Default
    BigDecimal loyaltyPoints = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalRescuedWeight = BigDecimal.ZERO;

    LocalDateTime lockedAt;
    LocalDateTime createAt;
    LocalDateTime updateAt;

    // ===== RELATIONSHIPS =====
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    Role role;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    List<Wallet> wallets;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<Order> buyerOrders;

    @OneToMany(mappedBy = "shop")
    @JsonIgnore
    List<Order> shopOrders;

    @OneToMany(mappedBy = "shipper")
    @JsonIgnore
    List<Order> shipperOrders;

    @OneToOne(mappedBy = "user")
    @JsonIgnore
    Cart cart;

    @OneToMany(mappedBy = "shop")
    @JsonIgnore
    List<Blog> blogs;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<Review> buyerReviews;

    @OneToMany(mappedBy = "shop")
    @JsonIgnore
    List<Voucher> vouchers;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    List<VoucherUser> voucherUsers;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<ReturnRequest> buyerReturns;

    @OneToMany(mappedBy = "shop")
    @JsonIgnore
    List<ReturnRequest> shopReturns;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<BuildPlan> buildPlans;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<ProfileUpdateRequest> profileUpdateRequests;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<UserNotification> userNotifications;

    @OneToMany(mappedBy = "shipper")
    @JsonIgnore
    List<ShipperLocation> shipperLocations;

    @OneToMany(mappedBy = "buyer")
    @JsonIgnore
    List<CheckoutBatch> checkoutBatches;

    @OneToMany(mappedBy = "admin")
    @JsonIgnore
    List<WithdrawRequest> adminWithdrawRequests;

    @OneToMany(mappedBy = "admin")
    @JsonIgnore
    List<Notification> adminNotifications;
}
