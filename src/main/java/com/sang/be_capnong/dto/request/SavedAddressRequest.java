package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SavedAddressRequest {

    String label; // e.g., "Nhà", "Công ty"

    @NotBlank(message = "Tên người nhận không được để trống")
    String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    String phone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    String address;

    Double latitude;

    Double longitude;

    Boolean isDefault;
}
