package com.sang.be_capnong.dto.request.wallet;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Admin duyệt / từ chối lệnh rút tiền */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawProcessRequest {
    String note;
}
