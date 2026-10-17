package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.DietPreference;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    String fullName;
    String phoneNumber;
    String address;
    String cccd;

    // Shop fields
    String shopName;
    String description;
    String logoUrl;
    String achievement;
    String license;
    String licenseImageUrl;
    String bankAccount;
    String bankAccountHolder;
    String bankName;

    // Shipper fields
    String vehicleNumber;
    String vehicleDocImageUrl;

    // Buyer fields
    DietPreference dietPreference;
}
