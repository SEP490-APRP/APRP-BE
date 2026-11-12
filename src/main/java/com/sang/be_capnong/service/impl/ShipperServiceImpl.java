package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ShipperLocationRequest;
import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ShipperLocationResponse;
import com.sang.be_capnong.dto.response.ShipperStatusResponse;
import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.ShipperLocation;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.OrderRepository;
import com.sang.be_capnong.repository.ShipperLocationRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.GoogleMapService;
import com.sang.be_capnong.service.ShipperService;
import com.sang.be_capnong.util.GeoUtils;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipperServiceImpl implements ShipperService {

    private final ShipperLocationRepository shipperLocationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final GoogleMapService googleMapService;

    /** Quá số giây này không có heartbeat thì coi như offline */
    @Value("${capnong.dispatch.heartbeat-timeout-seconds:300}")
    private int heartbeatTimeoutSeconds;

    private User getCurrentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    /** Mỗi shipper dùng 1 dòng; dữ liệu cũ có thể có nhiều dòng nên luôn lấy dòng mới nhất */
    private ShipperLocation findOrNew(User shipper) {
        return shipperLocationRepository.findFirstByShipperOrderByUpdatedAtDesc(shipper)
                .orElse(ShipperLocation.builder().shipper(shipper).build());
    }

    // ───────────────────────────── Trực tuyến ─────────────────────────────

    @Override
    @Transactional
    public void goOnlineCurrentShipper(ShipperStatusRequest request) {
        User shipper = getCurrentUser();
        ShipperLocation location = findOrNew(shipper);
        boolean wasOnline = Boolean.TRUE.equals(location.getOnline());
        LocalDateTime now = LocalDateTime.now();
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setUpdatedAt(now);
        location.setLastSeenAt(now);
        location.setOnline(true);
        shipperLocationRepository.save(location);
        if (!wasOnline) {
            log.info("Shipper {} went online at {}, {}", shipper.getId(), request.getLatitude(), request.getLongitude());
        }
    }

    @Override
    @Transactional
    public void goOfflineCurrentShipper() {
        User shipper = getCurrentUser();
        shipperLocationRepository.findFirstByShipperOrderByUpdatedAtDesc(shipper).ifPresent(location -> {
            // Không xoá dòng: đơn đang giao vẫn cần vị trí cuối để theo dõi
            location.setOnline(false);
            shipperLocationRepository.save(location);
        });
        log.info("Shipper {} went offline", shipper.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ShipperStatusResponse getCurrentShipperStatus() {
        User shipper = getCurrentUser();
        return shipperLocationRepository.findFirstByShipperOrderByUpdatedAtDesc(shipper)
                .filter(this::isFresh)
                .map(loc -> ShipperStatusResponse.builder()
                        .isOnline(true)
                        .latitude(loc.getLatitude())
                        .longitude(loc.getLongitude())
                        .updatedAt(loc.getUpdatedAt())
                        .build())
                .orElse(ShipperStatusResponse.builder().isOnline(false).build());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOnline(int shipperId) {
        return userRepository.findById(shipperId)
                .flatMap(shipperLocationRepository::findFirstByShipperOrderByUpdatedAtDesc)
                .map(this::isFresh)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public double[] getLastPosition(int shipperId) {
        return userRepository.findById(shipperId)
                .flatMap(shipperLocationRepository::findFirstByShipperOrderByUpdatedAtDesc)
                .filter(l -> l.getLatitude() != null && l.getLongitude() != null)
                .map(l -> new double[]{l.getLatitude(), l.getLongitude()})
                .orElse(null);
    }

    @Override
    @Transactional
    public int markStaleShippersOffline() {
        List<ShipperLocation> stale = shipperLocationRepository.findByOnlineTrueAndLastSeenAtBefore(
                LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds));
        stale.forEach(l -> l.setOnline(false));
        shipperLocationRepository.saveAll(stale);
        return stale.size();
    }

    private boolean isFresh(ShipperLocation l) {
        return Boolean.TRUE.equals(l.getOnline()) && l.getLastSeenAt() != null
                && l.getLastSeenAt().isAfter(LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds));
    }

    // ───────────────────────────── Vị trí realtime ─────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean canPublishLocation(Integer orderId, Principal principal) {
        if (orderId == null || principal == null) {
            return false;
        }
        Integer userId;
        try {
            userId = Integer.parseInt(principal.getName());
        } catch (NumberFormatException e) {
            return false;
        }
        return orderRepository.findById(orderId)
                .filter(o -> o.getShipper() != null && o.getShipper().getId() == userId)
                .filter(o -> o.getStatus() == OrderStatus.SHIPPING || o.getStatus() == OrderStatus.PICKED_UP)
                .isPresent();
    }

    @Override
    @Transactional
    public ShipperLocationResponse updateLocation(ShipperLocationRequest request, Principal principal) {
        if (!canPublishLocation(request.getOrderId(), principal)) {
            log.warn("[ShipperService] Rejected location update for order {} from principal {}",
                    request.getOrderId(), principal != null ? principal.getName() : null);
            return null;
        }
        User shipper = userRepository.findById(Integer.parseInt(principal.getName())).orElse(null);
        return shipper == null ? null : updateLocationByShipper(shipper, request);
    }

    @Override
    @Transactional
    public ShipperLocationResponse updateLocationByShipper(User shipper, ShipperLocationRequest request) {
        Order order = request.getOrderId() != null ? orderRepository.findById(request.getOrderId()).orElse(null) : null;
        ShipperLocation location = findOrNew(shipper);
        LocalDateTime now = LocalDateTime.now();
        location.setOrder(order);
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        if (request.getDeliveryPhase() != null && !request.getDeliveryPhase().isBlank()) {
            location.setDeliveryPhase(request.getDeliveryPhase());
        }
        location.setUpdatedAt(now);
        // Gửi vị trí cũng là heartbeat, nhưng không tự bật online
        location.setLastSeenAt(now);
        if (location.getOnline() == null) {
            location.setOnline(false);
        }
        shipperLocationRepository.save(location);

        return buildLocationResponse(order, shipper, location, request.getIsFake(), request.getGpsOff(), location.getDeliveryPhase());
    }

    @Override
    @Transactional
    public ShipperLocationResponse getShipperLocationByOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Phân quyền: chỉ Buyer của đơn, Shop của đơn, Shipper của đơn hoặc Admin mới được xem toạ độ
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId != null && !SecurityUtils.isAdmin()) {
            boolean isBuyer = order.getBuyer() != null && order.getBuyer().getId() == currentUserId;
            boolean isShop = order.getShop() != null && order.getShop().getId() == currentUserId;
            boolean isShipper = order.getShipper() != null && order.getShipper().getId() == currentUserId;
            if (!isBuyer && !isShop && !isShipper) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

        OrderStatus status = order.getStatus();
        boolean isFinished = status == OrderStatus.DELIVERED
                || status == OrderStatus.COMPLETED
                || status == OrderStatus.CANCELLED
                || status == OrderStatus.FAILED;

        if (isFinished) {
            // Đơn đã giao xong/kết thúc: Ngắt tracking realtime, chỉ trả toạ độ điểm nhận hàng tĩnh và ảnh PoD
            return buildFinishedLocationResponse(order);
        }

        User shipper = order.getShipper();
        ShipperLocation location = null;
        if (shipper != null) {
            // Chỉ lấy toạ độ nếu nó thuộc đúng đơn hàng này; không fallback sang vị trí đơn khác
            location = shipperLocationRepository.findByShipperAndOrder_OrderId(shipper, orderId).orElse(null);
            if (location == null) {
                // Kiểm tra xem vị trí mới nhất của shipper có đang không gắn với đơn nào khác không
                ShipperLocation latest = shipperLocationRepository.findFirstByShipperOrderByUpdatedAtDesc(shipper).orElse(null);
                if (latest != null && (latest.getOrder() == null || latest.getOrder().getOrderId().equals(orderId))) {
                    location = latest;
                }
            }
        }
        if (location == null) {
            location = shipperLocationRepository.findTopByOrder_OrderIdOrderByUpdatedAtDesc(orderId).orElse(null);
        }

        String phase = location != null ? location.getDeliveryPhase() : null;
        if (phase == null || phase.isBlank()) {
            phase = phaseOf(order.getStatus());
        }
        return buildLocationResponse(order, shipper, location, false, false, phase);
    }

    private ShipperLocationResponse buildFinishedLocationResponse(Order order) {
        Double shopLat = order.getShopLatitude() != null ? order.getShopLatitude() : 10.8500;
        Double shopLng = order.getShopLongitude() != null ? order.getShopLongitude() : 106.7720;
        Double destLat = order.getShippingLatitude() != null ? order.getShippingLatitude() : 10.7725;
        Double destLng = order.getShippingLongitude() != null ? order.getShippingLongitude() : 106.6578;

        User shipper = order.getShipper();
        String shipperName = shipper != null && shipper.getFullName() != null ? shipper.getFullName() : "Tài xế";
        String shipperPhone = shipper != null && shipper.getPhoneNumber() != null ? shipper.getPhoneNumber() : "";
        String vehicleNumber = shipper != null && shipper.getVehicleNumber() != null ? shipper.getVehicleNumber() : "";
        Integer shipperId = shipper != null ? shipper.getId() : null;

        String shopName = order.getShop() != null ? order.getShop().getShopName() : null;
        String shopAddress = order.getShop() != null ? order.getShop().getAddress() : null;

        LocalDateTime updatedAt = order.getDeliveredAt() != null
                ? order.getDeliveredAt()
                : (order.getUpdatedAt() != null ? order.getUpdatedAt() : LocalDateTime.now());

        String phase = order.getStatus() == OrderStatus.FAILED || order.getStatus() == OrderStatus.CANCELLED
                ? "FAILED"
                : "DELIVERED";

        List<String> proofs = parseProofUrls(order.getDeliveryProofUrls());

        return ShipperLocationResponse.builder()
                .orderId(order.getOrderId())
                .shipperId(shipperId)
                .shipperName(shipperName)
                .shipperPhone(shipperPhone)
                .vehicleNumber(vehicleNumber)
                .latitude(destLat)
                .longitude(destLng)
                .updatedAt(updatedAt)
                .shopLatitude(shopLat)
                .shopLongitude(shopLng)
                .shopAddress(shopAddress)
                .shopName(shopName)
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .destinationAddress(order.getShippingAddress())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .orderStatus(order.getStatus() != null ? order.getStatus().name() : "")
                .isFake(false)
                .gpsOff(false)
                .deliveryPhase(phase)
                .distanceKm(0.0)
                .estimatedMinutes(0)
                .trackingActive(false)
                .proofImageUrls(proofs)
                .build();
    }

    @Override
    @Transactional
    public void detachOrderFromShipperLocation(int orderId) {
        List<ShipperLocation> locations = shipperLocationRepository.findByOrder_OrderId(orderId);
        for (ShipperLocation loc : locations) {
            loc.setOrder(null);
            loc.setDeliveryPhase("DELIVERED");
            shipperLocationRepository.save(loc);
        }
    }

    private List<String> parseProofUrls(String joined) {
        if (joined == null || joined.isBlank()) return Collections.emptyList();
        return Arrays.stream(joined.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** SHIPPING = đang tới nhà vườn lấy hàng, PICKED_UP = đang giao cho khách */
    static String phaseOf(OrderStatus status) {
        if (status == null) return "TO_SHOP";
        return switch (status) {
            case PICKED_UP -> "TO_BUYER";
            case DELIVERED, COMPLETED -> "DELIVERED";
            default -> "TO_SHOP";
        };
    }

    private ShipperLocationResponse buildLocationResponse(Order order, User shipper, ShipperLocation location,
                                                          Boolean isFake, Boolean gpsOff, String deliveryPhase) {
        Double lat = location != null ? location.getLatitude() : null;
        Double lng = location != null ? location.getLongitude() : null;
        LocalDateTime updatedAt = location != null ? location.getUpdatedAt() : LocalDateTime.now();

        String shipperName = "Tài xế";
        String shipperPhone = "";
        String vehicleNumber = "";
        Integer shipperId = null;
        if (shipper != null) {
            shipperId = shipper.getId();
            shipperName = shipper.getFullName() != null ? shipper.getFullName() : "Tài xế";
            shipperPhone = shipper.getPhoneNumber() != null ? shipper.getPhoneNumber() : "";
            vehicleNumber = shipper.getVehicleNumber() != null ? shipper.getVehicleNumber() : "";
        }

        Double shopLat = order != null ? order.getShopLatitude() : null;
        Double shopLng = order != null ? order.getShopLongitude() : null;
        String shopAddress = null;
        String shopName = null;
        if (order != null && order.getShop() != null) {
            shopAddress = order.getShop().getAddress();
            shopName = order.getShop().getShopName();
        }
        if ((shopLat == null || shopLng == null) && shopAddress != null && !shopAddress.isBlank()) {
            double[] coords = geocode(shopAddress);
            if (coords != null) {
                shopLat = coords[0];
                shopLng = coords[1];
                order.setShopLatitude(shopLat);
                order.setShopLongitude(shopLng);
                orderRepository.save(order);
            }
        }
        if (shopLat == null || shopLng == null) {
            shopLat = 10.8500; // Linh Chiểu, TP. Thủ Đức
            shopLng = 106.7720;
        }

        Double destLat = order != null ? order.getShippingLatitude() : null;
        Double destLng = order != null ? order.getShippingLongitude() : null;
        String destAddress = order != null ? order.getShippingAddress() : null;
        if ((destLat == null || destLng == null) && destAddress != null && !destAddress.isBlank()) {
            double[] coords = geocode(destAddress);
            if (coords != null) {
                destLat = coords[0];
                destLng = coords[1];
                order.setShippingLatitude(destLat);
                order.setShippingLongitude(destLng);
                orderRepository.save(order);
            }
        }
        if (destLat == null || destLng == null) {
            destLat = 10.7725; // 268 Lý Thường Kiệt, Q10
            destLng = 106.6578;
        }

        double curLat = lat != null ? lat : shopLat;
        double curLng = lng != null ? lng : shopLng;

        // Quãng đường còn lại và ETA của chặng hiện tại
        Double remainingKm;
        Integer etaMinutes;
        if ("DELIVERED".equals(deliveryPhase)) {
            remainingKm = 0.0;
            etaMinutes = 0;
        } else {
            boolean toShop = !"TO_BUYER".equals(deliveryPhase);
            double targetLat = toShop ? shopLat : destLat;
            double targetLng = toShop ? shopLng : destLng;
            remainingKm = GeoUtils.roadKm(curLat, curLng, targetLat, targetLng);
            etaMinutes = GeoUtils.etaMinutes(remainingKm);
        }

        boolean isFinished = order != null && (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.FAILED
                || order.getStatus() == OrderStatus.CANCELLED);

        return ShipperLocationResponse.builder()
                .orderId(order != null ? order.getOrderId() : null)
                .shipperId(shipperId)
                .shipperName(shipperName)
                .shipperPhone(shipperPhone)
                .vehicleNumber(vehicleNumber)
                .latitude(curLat)
                .longitude(curLng)
                .updatedAt(updatedAt)
                .shopLatitude(shopLat)
                .shopLongitude(shopLng)
                .shopAddress(shopAddress)
                .shopName(shopName)
                .destinationLatitude(destLat)
                .destinationLongitude(destLng)
                .destinationAddress(destAddress)
                .recipientName(order != null ? order.getRecipientName() : null)
                .recipientPhone(order != null ? order.getRecipientPhone() : null)
                .orderStatus(order != null && order.getStatus() != null ? order.getStatus().name() : "")
                .isFake(isFake)
                .gpsOff(gpsOff)
                .deliveryPhase(deliveryPhase)
                .distanceKm(remainingKm)
                .estimatedMinutes(etaMinutes)
                .trackingActive(!isFinished)
                .proofImageUrls(parseProofUrls(order != null ? order.getDeliveryProofUrls() : null))
                .build();
    }

    private double[] geocode(String address) {
        try {
            double[] coords = googleMapService.geocodeAddress(address);
            return coords != null && coords.length == 2 ? coords : null;
        } catch (Exception e) {
            log.warn("Geocode address error: {}", e.getMessage());
            return null;
        }
    }
}
