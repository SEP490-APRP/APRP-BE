package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ShipperLocationRequest;
import com.sang.be_capnong.dto.response.ShipperLocationResponse;
import com.sang.be_capnong.service.ShipperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ShipperWebSocketController {

    private final ShipperService shipperService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/shipper/location")
    public void updateLocation(ShipperLocationRequest request, Principal principal) {
        if (request.getOrderId() == null) {
            log.warn("[WS] Missing orderId, skipping");
            return;
        }

        // Shipper tắt GPS -> broadcast gpsOff
        if (Boolean.TRUE.equals(request.getGpsOff())) {
            log.info("[WS] Shipper GPS OFF for order: {}", request.getOrderId());
            messagingTemplate.convertAndSend(
                    "/topic/order/" + request.getOrderId() + "/location",
                    Map.of("orderId", request.getOrderId(), "gpsOff", true)
            );
            return;
        }

        if (request.getLatitude() == null || request.getLongitude() == null) {
            log.warn("[WS] Missing lat/lng for order {}, skipping", request.getOrderId());
            return;
        }

        ShipperLocationResponse response = shipperService.updateLocation(request, principal);
        if (response != null) {
            // Broadcast tới tất cả subscribers của đơn hàng (Shipper, Shop, Buyer)
            messagingTemplate.convertAndSend(
                    "/topic/order/" + request.getOrderId() + "/location",
                    response
            );
        }
    }
}
