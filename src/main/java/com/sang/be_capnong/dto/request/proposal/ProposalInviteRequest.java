package com.sang.be_capnong.dto.request.proposal;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposalInviteRequest {
    List<Integer> shopIds;
    String message;
}
