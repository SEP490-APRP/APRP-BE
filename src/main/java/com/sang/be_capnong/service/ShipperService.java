package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ShipperStatusResponse;

public interface ShipperService {
    void goOnline(Integer shipperId, ShipperStatusRequest request);
    void goOffline(Integer shipperId);

    void goOnlineCurrentShipper(ShipperStatusRequest request);
    void goOfflineCurrentShipper();
    ShipperStatusResponse getCurrentShipperStatus();
}
