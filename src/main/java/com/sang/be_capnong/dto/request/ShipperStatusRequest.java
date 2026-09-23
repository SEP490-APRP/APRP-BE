package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperStatusRequest {
    @NotNull(message = "LATITUDE_REQUIRED")
    Double latitude;
    @NotNull(message = "LONGITUDE_REQUIRED")
    Double longitude;
}
