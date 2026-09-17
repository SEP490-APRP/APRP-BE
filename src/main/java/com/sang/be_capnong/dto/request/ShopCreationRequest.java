package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopCreationRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    @Size(max = 100, message = "EMAIL_TOO_LONG")
    String email;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    @NotBlank(message = "FULL_NAME_REQUIRED")
    String fullName;

    @NotBlank(message = "PHONE_NUMBER_REQUIRED")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "PHONE_NUMBER_INVALID")
    String phoneNumber;

    @NotBlank(message = "ADDRESS_REQUIRED")
    String address;

    @NotBlank(message = "SHOP_NAME_REQUIRED")
    String shopName;

    String description;
    String logoUrl;
    String achievement;
    String license;
    String licenseImageUrl;

    String bankAccount;
    String bankAccountHolder;
    String bankName;
}
