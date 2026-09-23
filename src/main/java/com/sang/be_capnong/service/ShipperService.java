package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ShipperStatusRequest;

public interface ShipperService {
    void goOnline(Integer shipperId, ShipperStatusRequest request);
    void goOffline(Integer shipperId);
}
