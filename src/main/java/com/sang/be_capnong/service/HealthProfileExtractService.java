package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ConfirmHealthProfileRequest;
import com.sang.be_capnong.dto.request.HealthProfileExtractRequest;
import com.sang.be_capnong.dto.response.HealthProfileExtractResponse;
import com.sang.be_capnong.dto.response.UserHealthProfileResponse;

public interface HealthProfileExtractService {
    HealthProfileExtractResponse extractProfile(HealthProfileExtractRequest request);
    UserHealthProfileResponse confirmProfileUpdate(int requestId, ConfirmHealthProfileRequest request);
}
