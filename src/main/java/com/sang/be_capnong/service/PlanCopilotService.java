package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.PlanCopilotApplyRequest;
import com.sang.be_capnong.dto.request.PlanCopilotChatRequest;
import com.sang.be_capnong.dto.response.BuildPlanResponse;
import com.sang.be_capnong.dto.response.PlanCopilotChatResponse;

public interface PlanCopilotService {
    PlanCopilotChatResponse chat(int planId, PlanCopilotChatRequest request);
    BuildPlanResponse applyAction(int planId, PlanCopilotApplyRequest request);
}
