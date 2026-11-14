package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.DeviceTokenRegisterRequest;
import com.sang.be_capnong.dto.request.DeviceTokenUnregisterRequest;

public interface DeviceTokenService {

    void registerDeviceToken(DeviceTokenRegisterRequest request);

    void unregisterDeviceToken(DeviceTokenUnregisterRequest request);
}

