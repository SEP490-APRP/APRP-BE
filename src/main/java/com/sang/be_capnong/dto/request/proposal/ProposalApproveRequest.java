package com.sang.be_capnong.dto.request.proposal;

import com.sang.be_capnong.dto.request.campaign.CampaignUpsertRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Admin duyệt 1 hoặc nhiều đơn gần nhau thành 1 chiến dịch nháp */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposalApproveRequest {
    List<Integer> proposalIds;
    CampaignUpsertRequest campaign;
    String note;
}
