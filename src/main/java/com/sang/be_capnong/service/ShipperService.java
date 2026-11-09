package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ShipperLocationRequest;
import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ShipperLocationResponse;
import com.sang.be_capnong.dto.response.ShipperStatusResponse;
import com.sang.be_capnong.entity.User;

import java.security.Principal;

public interface ShipperService {

    /** Bật trực tuyến hoặc gửi heartbeat (app gọi lại mỗi ~60s khi đang trực tuyến) */
    void goOnlineCurrentShipper(ShipperStatusRequest request);

    void goOfflineCurrentShipper();

    ShipperStatusResponse getCurrentShipperStatus();

    /** Đang bật nhận đơn và còn heartbeat trong capnong.dispatch.heartbeat-timeout-seconds */
    boolean isOnline(int shipperId);

    /** Vị trí gần nhất của shipper (lat, lng), null nếu chưa có */
    double[] getLastPosition(int shipperId);

    /** Scheduler: shipper mất heartbeat quá hạn -> chuyển offline. Trả về số shipper bị chuyển */
    int markStaleShippersOffline();

    /**
     * Người gửi (principal) có được phát vị trí cho đơn này không:
     * phải là shipper được gán và đơn đang SHIPPING / PICKED_UP.
     */
    boolean canPublishLocation(Integer orderId, Principal principal);

    ShipperLocationResponse updateLocation(ShipperLocationRequest request, Principal principal);

    ShipperLocationResponse updateLocationByShipper(User shipper, ShipperLocationRequest request);

    ShipperLocationResponse getShipperLocationByOrder(Integer orderId);
}
