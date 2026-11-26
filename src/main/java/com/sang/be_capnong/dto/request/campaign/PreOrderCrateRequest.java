package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderCrateRequest {
    /** Mọi thùng đã đóng của đơn (1..số thùng), thay cho lần trước */
    List<Integer> crates;
}
