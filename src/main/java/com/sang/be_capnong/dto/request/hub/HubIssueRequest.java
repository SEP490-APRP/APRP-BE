package com.sang.be_capnong.dto.request.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Hub báo đơn nhận bị thiếu hoặc hư */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubIssueRequest {
    /** Số thùng thiếu / hư */
    Integer crates;
    String note;
}
