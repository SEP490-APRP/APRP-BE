package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sang.be_capnong.enums.DietPreference;
import com.sang.be_capnong.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    int id;
    String email;
    String fullName;
    String phoneNumber;
    String address;
    String cccd;
    UserStatus status;
    RoleResponse role;

    // ===== SHOP FIELDS =====
    String shopName;
    Boolean shopActive;
    String bankAccount;
    String bankAccountHolder;
    String bankName;
    Double ratingAverage;
    String description;
    String logoUrl;
    String achievement;
    String license;
    String licenseImageUrl;

    // ===== SHIPPER FIELDS =====
    String vehicleNumber;
    String vehicleDocImageUrl;

    // ===== BUYER FIELDS =====
    DietPreference dietPreference;
    BigDecimal loyaltyPoints;
    BigDecimal totalRescuedWeight;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updateAt;
}