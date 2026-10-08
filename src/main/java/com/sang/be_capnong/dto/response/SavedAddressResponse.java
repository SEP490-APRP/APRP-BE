package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SavedAddressResponse {

    Long id;
    String label;
    String recipientName;
    String phone;
    String address;
    Double latitude;
    Double longitude;
    boolean isDefault;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
