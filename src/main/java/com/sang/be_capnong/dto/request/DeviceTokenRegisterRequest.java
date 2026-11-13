package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceTokenRegisterRequest {

    @NotBlank(message = "Device token is required")
    String deviceToken;

    String platform;

    String deviceName;

    String tokenType;
}

