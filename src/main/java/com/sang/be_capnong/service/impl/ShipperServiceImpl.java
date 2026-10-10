package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ShipperStatusResponse;
import com.sang.be_capnong.entity.ShipperLocation;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.ShipperLocationRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.ShipperService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShipperServiceImpl implements ShipperService {

    ShipperLocationRepository shipperLocationRepository;
    UserRepository userRepository;
    com.sang.be_capnong.repository.OrderRepository orderRepository;
    com.sang.be_capnong.service.GoogleMapService googleMapService;

    private User getCurrentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        int userId = Integer.parseInt(subject);
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public void goOnline(Integer shipperId, ShipperStatusRequest request) {
        log.info("Shipper {} went online at {}, {}", shipperId, request.getLatitude(), request.getLongitude());
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!"SHIPPER".equalsIgnoreCase(shipper.getRole().getName())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ShipperLocation location = shipperLocationRepository.findByShipper(shipper)
                .orElse(ShipperLocation.builder().shipper(shipper).build());

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setUpdatedAt(LocalDateTime.now());

        shipperLocationRepository.save(location);
    }

    @Override
    @Transactional
    public void goOffline(Integer shipperId) {
        log.info("Shipper {} went offline", shipperId);
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        shipperLocationRepository.deleteByShipper(shipper);
    }

    @Override
    @Transactional
    public void goOnlineCurrentShipper(ShipperStatusRequest request) {
        User shipper = getCurrentUser();
        goOnline(shipper.getId(), request);
    }

    @Override
    @Transactional
    public void goOfflineCurrentShipper() {
        User shipper = getCurrentUser();
        goOffline(shipper.getId());
    }

    @Override
    public ShipperStatusResponse getCurrentShipperStatus() {
        User shipper = getCurrentUser();
        return shipperLocationRepository.findByShipper(shipper)
                .map(loc -> ShipperStatusResponse.builder()
                        .isOnline(true)
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .updatedAt(loc.getUpdatedAt())
                        .build())
                .orElse(ShipperStatusResponse.builder()
                        .isOnline(false)
                        .build());
    }

    @Override
    @Transactional
    public com.sang.be_capnong.dto.response.ShipperLocationResponse updateLocationByShipper(User shipper, com.sang.be_capnong.dto.request.ShipperLocationRequest request) {
        com.sang.be_capnong.entity.Order order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId()).orElse(null);
        }

        ShipperLocation location = null;
        if (order != null) {
            location = shipperLocationRepository.findByShipperAndOrder_OrderId(shipper, order.getOrderId())
                    .orElse(null);
        }
        if (location == null) {
            location = shipperLocationRepository.findByShipper(shipper)
                    .orElse(ShipperLocation.builder().shipper(shipper).build());
        }

        location.setOrder(order);
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setUpdatedAt(LocalDateTime.now());
        shipperLocationRepository.save(location);

        return buildLocationResponse(order, shipper, location, request.getIsFake(), request.getGpsOff());
    }

    @Override
    public com.sang.be_capnong.dto.response.ShipperLocationResponse getShipperLocationByOrder(Integer orderId) {
        com.sang.be_capnong.entity.Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        User shipper = order.getShipper();
        ShipperLocation location = null;
        if (shipper != null) {
            location = shipperLocationRepository.findByShipperAndOrder_OrderId(shipper, orderId)
                    .orElse(null);
            if (location == null) {
                location = shipperLocationRepository.findByShipper(shipper).orElse(null);
            }
        }
        if (location == null) {
            location = shipperLocationRepository.findTopByOrder_OrderIdOrderByUpdatedAtDesc(orderId).orElse(null);
        }

        return buildLocationResponse(order, shipper, location, false, false);
    }

    private com.sang.be_capnong.dto.response.ShipperLocationResponse buildLocationResponse(
            com.sang.be_capnong.entity.Order order,
            User shipper,
            ShipperLocation location,
            Boolean isFake,
            Boolean gpsOff) {

        Double lat = location != null ? location.getLatitude() : null;
        Double lng = location != null ? location.getLongitude() : null;
        LocalDateTime updatedAt = location != null ? location.getUpdatedAt() : LocalDateTime.now();

        // Shop info
        User shop = order != null ? order.getShop() : null;
        Double shopLat = order != null ? order.getShopLatitude() : null;
        Double shopLng = order != null ? order.getShopLongitude() : null;
        String shopAddress = shop != null ? shop.getAddress() : null;
        String shopName = shop != null ? shop.getShopName() : null;

        if ((shopLat == null || shopLng == null) && shopAddress != null && !shopAddress.isBlank()) {
            try {
                double[] coords = googleMapService.geocodeAddress(shopAddress);
                if (coords != null && coords.length == 2) {
                    shopLat = coords[0];
                    shopLng = coords[1];
                    if (order != null) {
                        order.setShopLatitude(shopLat);
                        order.setShopLongitude(shopLng);
                        orderRepository.save(order);
                    }
                }
            } catch (Exception e) {
                log.warn("Geocode shop address error: {}", e.getMessage());
            }
        }
        if (shopLat == null || shopLng == null) {
            shopLat = 10.8500; // Linh Chiểu, TP. Thủ Đức
            shopLng = 106.7720;
        }

        // Destination info
        Double destLat = order != null ? order.getShippingLatitude() : null;
        Double destLng = order != null ? order.getShippingLongitude() : null;
        String destAddress = order != null ? order.getShippingAddress() : null;
        String recipientName = order != null ? order.getRecipientName() : null;
        String recipientPhone = order != null ? order.getRecipientPhone() : null;

        if ((destLat == null || destLng == null) && destAddress != null && !destAddress.isBlank()) {
            try {
                double[] coords = googleMapService.geocodeAddress(destAddress);
                if (coords != null && coords.length == 2) {
                    destLat = coords[0];
                    destLng = coords[1];
                    if (order != null) {
                        order.setShippingLatitude(destLat);
                        order.setShippingLongitude(destLng);
                        orderRepository.save(order);
                    }
                }
            } catch (Exception e) {
                log.warn("Geocode dest address error: {}", e.getMessage());
            }
        }
        if (destLat == null || destLng == null) {
            destLat = 10.7725; // 268 Lý Thường Kiệt, Q10
            destLng = 106.6578;
        }

        return com.sang.be_capnong.dto.response.ShipperLocationResponse.builder()
                .orderId(order != null ? order.getOrderId() : null)
                .shipperId(shipper != null ? shipper.getId() : null)
                .shipperName(shipper != null ? shipper.getFullName() : "Tài xế")
                .shipperPhone(shipper != null ? shipper.getPhoneNumber() : "")
                .vehicleNumber(shipper != null ? shipper.getVehicleNumber() : "")
                .latitude(lat != null ? lat : shopLat)
                .longitude(lng != null ? lng : shopLng)
                .updatedAt(updatedAt)
                .shopLatitude(shopLat)
                .shopLongitude(shopLng)
                .shopAddress(shopAddress)
                .shopName(shopName)
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .destinationAddress(destAddress)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .orderStatus(order != null && order.getStatus() != null ? order.getStatus().name() : "")
                .isFake(isFake)
                .gpsOff(gpsOff)
                .build();
    }
}
