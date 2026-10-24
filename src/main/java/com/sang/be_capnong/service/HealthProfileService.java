package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ManualHealthProfileUpdateRequest;
import com.sang.be_capnong.dto.response.UserHealthProfileResponse;

public interface HealthProfileService {
    UserHealthProfileResponse getMyHealthProfile();
    UserHealthProfileResponse updateMyHealthProfileManual(ManualHealthProfileUpdateRequest request);
}
