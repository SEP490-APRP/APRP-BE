package com.sang.be_capnong.dto.request.proposal;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** Shop được mời trả lời: tham gia kèm số kg dự kiến; admin trả về / từ chối kèm ghi chú */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposalReplyRequest {
    BigDecimal estimatedKg;
    String note;
}
