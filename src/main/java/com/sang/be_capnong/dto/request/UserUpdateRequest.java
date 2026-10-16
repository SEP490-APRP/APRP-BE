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

    // Shop fields
    String shopName;
    @Size(max = 500, message = "Mô tả nhà vườn không được vượt quá 500 ký tự")
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
