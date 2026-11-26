package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Admin nhắc shipper tới nhận các đơn còn chờ tại một hub */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubNotifyRequest {
    Integer hubId;
    List<Integer> shipperIds;
}
