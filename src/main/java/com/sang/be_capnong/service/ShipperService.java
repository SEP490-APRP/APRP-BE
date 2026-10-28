package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ShipperLocationRequest;
import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ShipperLocationResponse;
import com.sang.be_capnong.dto.response.ShipperStatusResponse;
import com.sang.be_capnong.entity.User;

public interface ShipperService {
    void goOnline(Integer shipperId, ShipperStatusRequest request);
    void goOffline(Integer shipperId);

    void goOnlineCurrentShipper(ShipperStatusRequest request);
    void goOfflineCurrentShipper();
    ShipperStatusResponse getCurrentShipperStatus();

    ShipperLocationResponse updateLocation(ShipperLocationRequest request, java.security.Principal principal);
    ShipperLocationResponse updateLocationByShipper(User shipper, ShipperLocationRequest request);
    ShipperLocationResponse getShipperLocationByOrder(Integer orderId);
}
