package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperStatusResponse {
    boolean isOnline;
    Double latitude;
    Double longitude;
    LocalDateTime updatedAt;
}
