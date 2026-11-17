package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.response.ShipperOfferMessage;
import com.sang.be_capnong.entity.DispatchOffer;
import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.ShipperLocation;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.repository.DispatchOfferRepository;
import com.sang.be_capnong.repository.OrderRepository;
import com.sang.be_capnong.repository.ShipperLocationRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.DispatchService;
import com.sang.be_capnong.service.GoogleMapService;
import com.sang.be_capnong.service.NotificationService;
import com.sang.be_capnong.service.WalletService;
import com.sang.be_capnong.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchServiceImpl implements DispatchService {

    public static final String QUEUE_OFFERS = "/queue/shipper-offers";
    public static final String TYPE_NEW_ORDER = "NEW_ORDER";
    public static final String TYPE_ORDER_TAKEN = "ORDER_TAKEN";

    private static final List<OrderStatus> BUSY_STATUSES = List.of(OrderStatus.SHIPPING, OrderStatus.PICKED_UP);

    private final OrderRepository orderRepository;
    private final ShipperLocationRepository shipperLocationRepository;
    private final DispatchOfferRepository dispatchOfferRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final GoogleMapService googleMapService;
    private final SimpMessagingTemplate messagingTemplate;

    /** Tự gọi qua proxy để REQUIRES_NEW có hiệu lực */
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private DispatchServiceImpl self;

    /** Bán kính từng đợt quét (km), tăng dần */
    @Value("${capnong.dispatch.radii-km:3,6,10}")
    private double[] radiiKm;

    /** Khoảng cách giữa 2 đợt quét */
    @Value("${capnong.dispatch.wave-interval-seconds:60}")
    private int waveIntervalSeconds;

    @Value("${capnong.dispatch.heartbeat-timeout-seconds:300}")
    private int heartbeatTimeoutSeconds;

    /** Đơn READY quá số phút này chưa ai nhận thì báo Shop + Admin (1 lần) */
    @Value("${capnong.dispatch.no-taker-alert-minutes:15}")
    private int noTakerAlertMinutes;

    // ───────────────────────────── Điều phối ─────────────────────────────

    @Override
    public void dispatchAfterCommit(int orderId) {
        afterCommit(() -> self.dispatchNextWave(orderId));
    }

    /** Quét 1 đợt cho đơn (đợt tiếp theo, hoặc quét lại bán kính lớn nhất). Public để chạy qua proxy */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchNextWave(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.READY || order.getShipper() != null) {
            return;
        }
        int wave = Math.min(order.getDispatchWave() == null ? 1 : order.getDispatchWave() + 1, radiiKm.length);
        double radius = radiiKm[wave - 1];
        LocalDateTime now = LocalDateTime.now();
        if (order.getReadyAt() == null) {
            order.setReadyAt(now);
        }

        double[] shop = shopPosition(order);
        Set<Integer> excluded = new HashSet<>(dispatchOfferRepository.findShipperIdsByOrderId(orderId));
        excluded.addAll(orderRepository.findShipperIdsWithStatusIn(BUSY_STATUSES));
        boolean cod = "COD".equalsIgnoreCase(order.getPaymentMethod());
        BigDecimal codAmount = cod && order.getTotalAmount() != null
                ? order.getTotalAmount().subtract(order.getWalletPaidAmount() != null ? order.getWalletPaidAmount() : BigDecimal.ZERO).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        List<DispatchOffer> offers = new ArrayList<>();
        for (ShipperLocation loc : shipperLocationRepository.findOnlineSince(now.minusSeconds(heartbeatTimeoutSeconds))) {
            User shipper = loc.getShipper();
            if (excluded.contains(shipper.getId()) || shipper.getStatus() != UserStatus.ACTIVE
                    || loc.getLatitude() == null || loc.getLongitude() == null) {
                continue;
            }
            excluded.add(shipper.getId());
            // Không biết toạ độ nhà vườn thì mời mọi shipper đang online
            Double distance = shop == null ? null
                    : GeoUtils.roadKm(loc.getLatitude(), loc.getLongitude(), shop[0], shop[1]);
            if (distance != null && distance > radius) {
                continue;
            }
            // Đơn COD: chỉ mời shipper đủ tiền ký quỹ (shipper khác vẫn thấy đơn trong danh sách)
            if (cod && walletService.getShipperAvailableBalance(shipper.getId()).compareTo(codAmount) < 0) {
                continue;
            }
            offers.add(DispatchOffer.builder()
                    .order(order).shipper(shipper).wave(wave).distanceKm(distance).createdAt(now).build());
        }
        dispatchOfferRepository.saveAll(offers);
        order.setDispatchWave(wave);
        order.setLastDispatchedAt(now);
        orderRepository.save(order);
        log.info("[Dispatch] Order #{} wave {} ({} km): invited {} shipper(s)", orderId, wave, radius, offers.size());

        for (DispatchOffer offer : offers) {
            User shipper = offer.getShipper();
            String where = offer.getDistanceKm() != null ? " cách bạn " + offer.getDistanceKm() + " km" : "";
            notificationService.sendNotificationToUser(shipper,
                    "Có đơn mới gần bạn",
                    "Đơn #" + orderId + " tại " + shopName(order) + where
                            + (cod ? ". Đơn COD, cần ký quỹ " + vnd(codAmount) : ". Đơn đã thanh toán online"),
                    "NEW_ORDER_OFFER", orderId);
            sendOffer(shipper.getId(), offerMessage(TYPE_NEW_ORDER, order, offer.getDistanceKm(), wave, codAmount));
        }
    }

    @Override
    public void runDispatchCycle() {
        LocalDateTime now = LocalDateTime.now();
        for (Order order : orderRepository.findByStatusAndShipperIsNull(OrderStatus.READY)) {
            try {
                boolean due = order.getDispatchWave() == null || order.getLastDispatchedAt() == null
                        || !order.getLastDispatchedAt().isAfter(now.minusSeconds(waveIntervalSeconds));
                if (due) {
                    self.dispatchNextWave(order.getOrderId());
                }
                if (order.getDispatchAlertedAt() == null && order.getReadyAt() != null
                        && order.getReadyAt().isBefore(now.minusMinutes(noTakerAlertMinutes))) {
                    self.alertNoTaker(order.getOrderId());
                }
            } catch (Exception e) {
                log.error("[Dispatch] Order #{} cycle failed: {}", order.getOrderId(), e.getMessage(), e);
            }
        }
    }

    /** Báo Shop + Admin đơn READY lâu chưa có tài xế (1 lần). Public để chạy qua proxy */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alertNoTaker(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.READY || order.getShipper() != null
                || order.getDispatchAlertedAt() != null) {
            return;
        }
        order.setDispatchAlertedAt(LocalDateTime.now());
        orderRepository.save(order);
        String msg = "Đơn #" + orderId + " đã sẵn sàng hơn " + noTakerAlertMinutes
                + " phút nhưng chưa có tài xế nhận. Hệ thống vẫn tiếp tục tìm tài xế.";
        if (order.getShop() != null) {
            notificationService.sendNotificationToUser(order.getShop(), "Chưa có tài xế nhận đơn", msg, "DISPATCH_TIMEOUT", orderId);
        }
        for (User admin : userRepository.findByRole_NameIn(List.of("ADMIN"))) {
            notificationService.sendNotificationToUser(admin, "Đơn chưa có tài xế", msg, "DISPATCH_TIMEOUT", orderId);
        }
        log.warn("[Dispatch] Order #{} has no taker after {} minutes", orderId, noTakerAlertMinutes);
    }

    @Override
    public void notifyOrderTakenAfterCommit(int orderId, int takenByShipperId) {
        afterCommit(() -> {
            try {
                for (Integer shipperId : dispatchOfferRepository.findShipperIdsByOrderId(orderId)) {
                    if (shipperId != takenByShipperId) {
                        sendOffer(shipperId, ShipperOfferMessage.builder().type(TYPE_ORDER_TAKEN).orderId(orderId).build());
                    }
                }
            } catch (Exception e) {
                log.warn("[Dispatch] Cannot notify ORDER_TAKEN for #{}: {}", orderId, e.getMessage());
            }
        });
    }

    // ───────────────────────────── Thông báo trạng thái ─────────────────────────────

    @Override
    public void notifyStatusChangedAfterCommit(int orderId) {
        afterCommit(() -> {
            try {
                self.sendStatusNotifications(orderId);
            } catch (Exception e) {
                log.warn("[Dispatch] Cannot send status notification for #{}: {}", orderId, e.getMessage());
            }
        });
    }

    /** Public để chạy qua proxy */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendStatusNotifications(int orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() == null) {
            return;
        }
        String shipper = order.getShipper() != null && order.getShipper().getFullName() != null
                ? order.getShipper().getFullName() : "Tài xế";
        String buyerMsg;
        String shopMsg;
        switch (order.getStatus()) {
            case READY -> {
                buyerMsg = "Đơn #" + orderId + " đã được nhà vườn đóng gói, đang tìm tài xế.";
                shopMsg = null;
            }
            case SHIPPING -> {
                buyerMsg = shipper + " đã nhận đơn #" + orderId + " và đang tới nhà vườn lấy hàng.";
                shopMsg = shipper + " đã nhận đơn #" + orderId + " và đang tới lấy hàng. Bạn có thể theo dõi trên bản đồ.";
            }
            case PICKED_UP -> {
                buyerMsg = "Đơn #" + orderId + " đã được lấy hàng và đang giao tới bạn. Mở bản đồ để theo dõi.";
                shopMsg = shipper + " đã lấy hàng đơn #" + orderId + ".";
            }
            case DELIVERED -> {
                buyerMsg = "Đơn #" + orderId + " đã giao thành công. Hãy bấm \"Đã nhận hàng\" nếu hàng ổn nhé.";
                shopMsg = "Đơn #" + orderId + " đã giao thành công cho khách.";
            }
            case FAILED -> {
                buyerMsg = "Đơn #" + orderId + " giao không thành công. Đơn đã được huỷ"
                        + (order.getRefundAmount() != null && order.getRefundAmount().signum() > 0
                        ? ", sàn sẽ hoàn " + vnd(order.getRefundAmount()) + " cho bạn." : ".");
                shopMsg = "Đơn #" + orderId + " giao thất bại, hàng được hoàn về kho.";
            }
            default -> {
                return;
            }
        }
        if (buyerMsg != null && order.getBuyer() != null) {
            notificationService.sendNotificationToUser(order.getBuyer(), "Cập nhật đơn hàng", buyerMsg, "ORDER_STATUS", orderId);
        }
        if (shopMsg != null && order.getShop() != null) {
            notificationService.sendNotificationToUser(order.getShop(), "Cập nhật đơn hàng", shopMsg, "ORDER_STATUS", orderId);
        }

        // Broadcast WebSocket cập nhật trạng thái tracking cho các bên theo dõi (Buyer, Shop, Shipper)
        try {
            boolean trackingActive = order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.PICKED_UP;
            String phase = switch (order.getStatus()) {
                case PICKED_UP -> "TO_BUYER";
                case DELIVERED, COMPLETED -> "DELIVERED";
                case FAILED, CANCELLED -> "FAILED";
                default -> "TO_SHOP";
            };
            messagingTemplate.convertAndSend(
                    "/topic/order/" + orderId + "/location",
                    java.util.Map.of(
                            "orderId", orderId,
                            "orderStatus", order.getStatus().name(),
                            "deliveryPhase", phase,
                            "trackingActive", trackingActive,
                            "distanceKm", 0.0,
                            "estimatedMinutes", 0
                    )
            );
        } catch (Exception wsEx) {
            log.warn("[Dispatch] Cannot send WS location update for #{}: {}", orderId, wsEx.getMessage());
        }
    }

    // ───────────────────────────── Tiện ích ─────────────────────────────

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private void sendOffer(int shipperId, ShipperOfferMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(shipperId), QUEUE_OFFERS, message);
        } catch (Exception e) {
            log.warn("[Dispatch] Cannot send offer to shipper {}: {}", shipperId, e.getMessage());
        }
    }

    private ShipperOfferMessage offerMessage(String type, Order order, Double distanceToShop, int wave, BigDecimal codAmount) {
        Double tripKm = null;
        if (order.getShopLatitude() != null && order.getShopLongitude() != null
                && order.getShippingLatitude() != null && order.getShippingLongitude() != null) {
            tripKm = GeoUtils.roadKm(order.getShopLatitude(), order.getShopLongitude(),
                    order.getShippingLatitude(), order.getShippingLongitude());
        }
        return ShipperOfferMessage.builder()
                .type(type)
                .orderId(order.getOrderId())
                .shopName(shopName(order))
                .shopAddress(order.getShop() != null ? order.getShop().getAddress() : null)
                .shippingAddress(order.getShippingAddress())
                .distanceToShopKm(distanceToShop)
                .distanceKm(tripKm)
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .codAmount(codAmount)
                .wave(wave)
                .build();
    }

    private double[] shopPosition(Order order) {
        if (order.getShopLatitude() != null && order.getShopLongitude() != null) {
            return new double[]{order.getShopLatitude(), order.getShopLongitude()};
        }
        String address = order.getShop() != null ? order.getShop().getAddress() : null;
        if (address == null || address.isBlank()) {
            return null;
        }
        try {
            double[] coords = googleMapService.geocodeAddress(address);
            if (coords != null && coords.length == 2) {
                order.setShopLatitude(coords[0]);
                order.setShopLongitude(coords[1]);
                return coords;
            }
        } catch (Exception e) {
            log.warn("[Dispatch] Geocode shop address failed for order #{}: {}", order.getOrderId(), e.getMessage());
        }
        return null;
    }

    private static String shopName(Order order) {
        if (order.getShop() == null) return "nhà vườn";
        return order.getShop().getShopName() != null ? order.getShop().getShopName() : order.getShop().getFullName();
    }

    private static String vnd(BigDecimal v) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0fđ", v == null ? BigDecimal.ZERO : v);
    }
}
