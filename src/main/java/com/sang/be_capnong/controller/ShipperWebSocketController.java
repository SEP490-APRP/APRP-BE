package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ShipperLocationRequest;
import com.sang.be_capnong.dto.response.ShipperLocationResponse;
import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.repository.OrderRepository;
import com.sang.be_capnong.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

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

        User shipper = null;
        if (principal != null) {
            try {
                int userId = Integer.parseInt(principal.getName());
                shipper = userRepository.findById(userId).orElse(null);
            } catch (Exception e) {
                log.warn("[WS] Could not parse principal userId: {}", principal.getName());
            }
        }

        // Fallback: nếu không có principal thì lấy shipper được gán cho order
        if (shipper == null && request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId()).orElse(null);
            if (order != null && order.getShipper() != null) {
                shipper = userRepository.findById(order.getShipper().getId()).orElse(null);
            }
        }

        if (shipper == null) {
            log.warn("[WS] No shipper found for location update on order {}", request.getOrderId());
            return;
        }

        log.debug("[WS] Location update: orderId={}, shipperId={}, lat={}, lng={}, isFake={}",
                request.getOrderId(), shipper.getId(), request.getLatitude(), request.getLongitude(), request.getIsFake());

        ShipperLocationResponse response = shipperService.updateLocationByShipper(shipper, request);

        // Broadcast tới tất cả subscribers của đơn hàng (Shipper, Shop, Buyer)
        messagingTemplate.convertAndSend(
                "/topic/order/" + request.getOrderId() + "/location",
                response
        );
    }
}
