package com.sang.be_capnong.service.campaign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import com.sang.be_capnong.dto.response.hub.HubTripBox;
import com.sang.be_capnong.dto.request.hub.HubTripBoxRequest;
import com.sang.be_capnong.enums.PreOrderStatus;
import com.sang.be_capnong.dto.request.hub.HubTripStop;
import com.sang.be_capnong.dto.request.hub.HubTripSyncRequest;
import com.sang.be_capnong.dto.response.hub.HubTripResponse;
import com.sang.be_capnong.entity.Campaign;
import com.sang.be_capnong.entity.HubTrip;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.CampaignStatus;
import com.sang.be_capnong.enums.HubTripStatus;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.CampaignRepository;
import com.sang.be_capnong.repository.HubTripRepository;
import com.sang.be_capnong.repository.PreOrderRepository;
import com.sang.be_capnong.entity.PreOrder;
import com.sang.be_capnong.util.GeoUtils;
import java.util.LinkedHashMap;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.enums.VehicleType;
import com.sang.be_capnong.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Chuyến xe chở hàng từ điểm tập kết ra hub:
 * admin lập kế hoạch (kêu gọi trước, chốt sau khi cân), tài xế nhận chuyến / lấy hàng / giao tới hub.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HubTripService {

    /** Cột notifications.title dài tối đa 50 ký tự */
    private static final int TITLE_MAX = 50;
    private static final String NOTI_TYPE = "HUB_TRIP";

    HubTripRepository tripRepository;
    CampaignRepository campaignRepository;
    UserRepository userRepository;
    NotificationService notificationService;
    ObjectMapper objectMapper;
    HubTripFeeCalculator feeCalculator;
    CampaignEscrowService escrowService;
    CampaignShippingCalculator shippingCalculator;
    PreOrderRepository preOrderRepository;

    /** Phí dịch vụ trên cước xe ra hub: mặc định 0, tài xế nhận đủ cước theo bảng giá xe tải */
    @Value("${capnong.campaign.hubtrip.fee-rate:0}")
    @lombok.experimental.NonFinal
    BigDecimal serviceFeeRate = BigDecimal.ZERO;

    // ═════════════════════════════ ADMIN ═════════════════════════════

    @Transactional(readOnly = true)
    public List<HubTripResponse> listForCampaign(int campaignId) {
        return tripRepository.findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(campaignId, HubTripStatus.CANCELLED).stream()
                .map(this::toResponse).toList();
    }

    /**
     * Đồng bộ kế hoạch xe với các chuyến đã lưu theo số thứ tự xe. Chuyến đã lấy hàng / đã giao không bị đổi.
     * Chuyến không còn trong kế hoạch bị huỷ (báo tài xế nếu đã nhận).
     */
    @Transactional
    public List<HubTripResponse> sync(int campaignId, HubTripSyncRequest request) {
        Campaign c = campaignRepository.findById(campaignId).orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));
        if (request == null) throw new AppException(ErrorCode.CAMPAIGN_INVALID_DATA);
        boolean confirm = Boolean.TRUE.equals(request.getConfirmed());
        // Đóng gói hết rồi mới chốt xe: tài xế tới là có thùng để nhận
        if (confirm && request.getTrips() != null) {
            List<Integer> ids = request.getTrips().stream().filter(t -> t.getStops() != null).flatMap(t -> t.getStops().stream())
                    .filter(s -> s.getOrderIds() != null).flatMap(s -> s.getOrderIds().stream()).toList();
            if (preOrderRepository.findAllById(ids).stream().anyMatch(p -> p.getStatus() == PreOrderStatus.DEPOSIT_PAID)) {
                throw new AppException(ErrorCode.CAMPAIGN_ORDERS_NOT_PACKED);
            }
        }
        LocalDate pickup = parseDate(request.getPickupDate());
        LocalDateTime now = LocalDateTime.now();
        List<HubTrip> existing = tripRepository.findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(campaignId, HubTripStatus.CANCELLED);
        Map<Integer, HubTrip> byNo = existing.stream().collect(Collectors.toMap(HubTrip::getTripNo, t -> t, (a, b) -> a));
        Set<Integer> planned = new HashSet<>();
        // Cước chặng cuối (hub -> địa chỉ) của các đơn trên những chuyến chưa lấy hàng
        List<HubTripSyncRequest.Trip> unlocked = (request.getTrips() == null ? List.<HubTripSyncRequest.Trip>of() : request.getTrips()).stream()
                .filter(pt -> pt.getTripNo() != null && pt.getTripNo() >= 1)
                .filter(pt -> {
                    HubTrip cur = byNo.get(pt.getTripNo());
                    return cur == null || (cur.getStatus() != HubTripStatus.IN_TRANSIT && cur.getStatus() != HubTripStatus.DELIVERED);
                }).toList();
        setLastMilePay(c, unlocked);

        for (HubTripSyncRequest.Trip pt : request.getTrips() == null ? List.<HubTripSyncRequest.Trip>of() : request.getTrips()) {
            if (pt.getTripNo() == null || pt.getTripNo() < 1) continue;
            planned.add(pt.getTripNo());
            HubTrip t = byNo.get(pt.getTripNo());
            User chosen = pt.getDriverId() == null ? null : findDriver(pt.getDriverId());
            if (t == null) {
                t = HubTrip.builder().campaign(c).tripNo(pt.getTripNo()).status(HubTripStatus.OPEN).createdAt(now).build();
            } else if (t.getStatus() == HubTripStatus.IN_TRANSIT || t.getStatus() == HubTripStatus.DELIVERED) {
                continue; // đã lấy hàng, giữ nguyên
            }
            // Tài xế vừa nhận trong lúc admin đang xếp lại xe: không đổi lộ trình của chuyến đã có người nhận
            // (bớt hub vì đơn bị huỷ do thiếu hàng thì được, thêm hub mới thì không)
            if (t.getDriver() != null && !hubIds(readStops(t.getStopsJson())).containsAll(hubIds(pt.getStops()))) {
                throw new AppException(ErrorCode.HUB_TRIP_TAKEN);
            }
            // Lộ trình đổi sang hub khác thì coi như chuyến mới: tài xế đã từ chối được xem lại
            if (!hubIds(readStops(t.getStopsJson())).equals(hubIds(pt.getStops()))) t.setDeclinedDriverIds(null);
            if (chosen != null) setDeclined(t, chosen.getId(), false);
            if (pt.getVehicleKg() != null) t.setVehicleKg(pt.getVehicleKg());
            BigDecimal oldKg = t.getTotalKg();
            t.setPickupDate(pickup != null ? pickup : t.getPickupDate());
            t.setTotalKg(BigDecimal.valueOf(pt.getTotalKg() == null ? 0 : pt.getTotalKg()).setScale(2, RoundingMode.HALF_UP));
            t.setStopsJson(writeStops(pt.getStops()));
            Double km = feeCalculator.routeKm(c.getCollectionSiteLatitude(), c.getCollectionSiteLongitude(), pt.getStops());
            t.setRouteKm(km);
            // Sàn trả cước theo bảng giá xe tải
            t.setFeeAmount(feeCalculator.fee(km, pt.getTotalKg() == null ? 0 : pt.getTotalKg()));
            if (chosen != null) requireCapacity(chosen, t.getTotalKg());
            if (confirm) t.setConfirmed(true);
            t.setUpdatedAt(now);
            if (chosen != null && (t.getDriver() == null || t.getDriver().getId() != chosen.getId())) {
                User old = t.getDriver();
                t.setDriver(chosen);
                t.setStatus(HubTripStatus.ACCEPTED);
                t.setAcceptedAt(now);
                if (old != null) notify(old, "Xe chở hàng ra hub đã đổi tài xế", "Chuyến Xe " + t.getTripNo() + " của chiến dịch \"" + c.getTitle() + "\" đã được giao cho tài xế khác.", c.getCampaignId());
                notify(chosen, "Bạn được giao xe chở hàng ra hub",
                        tripText(c, t) + (t.isConfirmed() ? " Bạn có thể tới điểm tập kết lấy hàng." : " Kế hoạch dự kiến, CapNong xác nhận sau khi cân hàng."), c.getCampaignId());
            } else if (t.getDriver() != null && confirm) {
                notify(t.getDriver(), "Chốt xe chở hàng ra hub", tripText(c, t) + " Kế hoạch đã chốt, bạn có thể tới điểm tập kết lấy hàng.", c.getCampaignId());
            } else if (t.getDriver() != null && oldKg != null && oldKg.compareTo(t.getTotalKg()) != 0) {
                notify(t.getDriver(), "Xe ra hub đổi khối lượng", tripText(c, t) + " Trước đó " + Math.round(oldKg.doubleValue())
                        + " kg; cước mới " + t.getFeeAmount().setScale(0, RoundingMode.HALF_UP).toPlainString() + "đ.", c.getCampaignId());
            }
            tripRepository.save(t);
        }
        // Chuyến không còn trong kế hoạch: huỷ
        for (HubTrip t : existing) {
            if (planned.contains(t.getTripNo()) || t.getStatus() == HubTripStatus.IN_TRANSIT || t.getStatus() == HubTripStatus.DELIVERED) continue;
            if (t.getDriver() != null) {
                notify(t.getDriver(), "Chuyến xe chở hàng ra hub đã huỷ", "Kế hoạch xe của chiến dịch \"" + c.getTitle() + "\" đã thay đổi, chuyến Xe " + t.getTripNo() + " không còn.", c.getCampaignId());
            }
            t.setStatus(HubTripStatus.CANCELLED);
            t.setUpdatedAt(now);
            tripRepository.save(t);
        }
        return listForCampaign(campaignId);
    }

    /** Đã lập chuyến xe ra hub và gọi tài xế (chưa cần tài xế nhận) */
    @Transactional(readOnly = true)
    public boolean trucksCalled(int campaignId) {
        return !tripRepository.findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(campaignId, HubTripStatus.CANCELLED).isEmpty();
    }

    /** Đơn đã đóng gói đang ở đâu theo chuyến xe ra hub; đơn không đi theo chuyến nào coi như đã ở hub */
    @Transactional(readOnly = true)
    public String hubStageOf(int campaignId, int orderId) {
        String stage = "AT_HUB";
        for (HubTrip t : tripRepository.findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(campaignId, HubTripStatus.CANCELLED)) {
            HubTripStop stop = readStops(t.getStopsJson()).stream()
                    .filter(s -> s.getOrderIds() != null && s.getOrderIds().contains(orderId)).findFirst().orElse(null);
            if (stop == null) continue;
            if (arrived(t, stop)) return "AT_HUB";
            stage = t.getStatus() == HubTripStatus.IN_TRANSIT ? "ON_THE_WAY" : "AT_SITE";
        }
        return stage;
    }

    /** Chuyến xong hoặc tài xế đã báo giao tới hub này */
    private static boolean arrived(HubTrip t, HubTripStop s) {
        return t.getStatus() == HubTripStatus.DELIVERED || s.getArrivedAt() != null;
    }

    /** Đơn đã được xe tải giao tới hub */
    @Transactional(readOnly = true)
    public Set<Integer> orderIdsArrived(int campaignId) {
        Set<Integer> arrived = new HashSet<>();
        for (HubTrip t : tripRepository.findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(campaignId, HubTripStatus.CANCELLED)) {
            for (HubTripStop s : readStops(t.getStopsJson())) {
                if (arrived(t, s) && s.getOrderIds() != null) arrived.addAll(s.getOrderIds());
            }
        }
        return arrived;
    }

    /** Đơn nằm trên chuyến xe ra hub chưa tới nơi: shipper chặng cuối chưa nhận được. Đơn không đi theo chuyến nào thì không tính */
    @Transactional(readOnly = true)
    public Set<Integer> orderIdsOnTheWay(int campaignId) {
        Set<Integer> waiting = new HashSet<>();
        Set<Integer> arrived = new HashSet<>();
        for (HubTrip t : tripRepository.findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(campaignId, HubTripStatus.CANCELLED)) {
            for (HubTripStop s : readStops(t.getStopsJson())) {
                if (s.getOrderIds() != null) (arrived(t, s) ? arrived : waiting).addAll(s.getOrderIds());
            }
        }
        waiting.removeAll(arrived);
        return waiting;
    }

    // ═════════════════════════════ TÀI XẾ ═════════════════════════════

    /** Chuyến chưa có tài xế của các chiến dịch đang chuẩn bị hoặc đang gom hàng; chuyến mình đã từ chối có cờ declined */
    @Transactional(readOnly = true)
    public List<HubTripResponse> openTrips() {
        int me = currentUserId();
        int capacity = VehicleType.capacityKgOf(userRepository.findById(me).map(User::getVehicleType).orElse(null));
        // Chỉ hiện chuyến mà xe của tài xế chở được (xe máy không thấy chuyến chở cả trăm kg)
        return tripRepository.findByStatusAndCampaign_StatusInOrderByPickupDateAscTripNoAsc(HubTripStatus.OPEN,
                List.of(CampaignStatus.THRESHOLD_MET, CampaignStatus.FULFILLING)).stream()
                .filter(t -> t.getTotalKg() != null && capacity >= t.getTotalKg().doubleValue() && capacity >= feeCalculator.getMinVehicleKg())
                .map(t -> {
                    HubTripResponse r = toResponse(t);
                    r.setDeclined(declinedIds(t).contains(me));
                    return r;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<HubTripResponse> myTrips() {
        return tripRepository.findByDriver_IdAndStatusNotOrderByPickupDateDescTripNoAsc(currentUserId(), HubTripStatus.CANCELLED).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public HubTripResponse accept(int tripId) {
        User me = findDriver(currentUserId());
        HubTrip t = lock(tripId);
        if (t.getStatus() != HubTripStatus.OPEN) throw new AppException(ErrorCode.HUB_TRIP_TAKEN);
        requireCapacity(me, t.getTotalKg());
        LocalDateTime now = LocalDateTime.now();
        t.setDriver(me);
        t.setStatus(HubTripStatus.ACCEPTED);
        t.setAcceptedAt(now);
        t.setUpdatedAt(now);
        setDeclined(t, me.getId(), false);
        tripRepository.save(t);
        notifyAdmins("Tài xế nhận xe chở hàng ra hub", nameOf(me) + " nhận Xe " + t.getTripNo() + " của chiến dịch \"" + t.getCampaign().getTitle() + "\".", t.getCampaign().getCampaignId());
        return toResponse(t);
    }

    /** Không nhận chuyến đang chờ: ẩn khỏi danh sách chính của tài xế này, vẫn nhận lại được nếu chưa ai nhận */
    @Transactional
    public HubTripResponse decline(int tripId) {
        User me = findDriver(currentUserId());
        HubTrip t = lock(tripId);
        if (t.getStatus() != HubTripStatus.OPEN) throw new AppException(ErrorCode.HUB_TRIP_INVALID_STATUS);
        if (!declinedIds(t).contains(me.getId())) {
            setDeclined(t, me.getId(), true);
            t.setUpdatedAt(LocalDateTime.now());
            tripRepository.save(t);
            notifyAdmins("Tài xế không nhận chuyến ra hub", nameOf(me) + " không nhận Xe " + t.getTripNo() + " của chiến dịch \"" + t.getCampaign().getTitle() + "\".", t.getCampaign().getCampaignId());
        }
        HubTripResponse r = toResponse(t);
        r.setDeclined(true);
        return r;
    }

    /** Trả chuyến lại để người khác nhận (chỉ khi chưa lấy hàng) */
    @Transactional
    public HubTripResponse release(int tripId) {
        HubTrip t = lockMine(tripId);
        if (t.getStatus() != HubTripStatus.ACCEPTED) throw new AppException(ErrorCode.HUB_TRIP_INVALID_STATUS);
        User me = t.getDriver();
        t.setDriver(null);
        t.setStatus(HubTripStatus.OPEN);
        t.setAcceptedAt(null);
        setDeclined(t, me.getId(), true);
        t.setUpdatedAt(LocalDateTime.now());
        tripRepository.save(t);
        notifyAdmins("Tài xế trả chuyến xe chở hàng ra hub", nameOf(me) + " trả Xe " + t.getTripNo() + " của chiến dịch \"" + t.getCampaign().getTitle() + "\", chuyến đang chờ tài xế khác.", t.getCampaign().getCampaignId());
        return toResponse(t);
    }

    /** Tài xế đánh dấu các thùng của một đơn đã nhận lên xe (danh sách số thùng, thay cho lần trước) */
    @Transactional
    public HubTripResponse loadBoxes(int tripId, HubTripBoxRequest request) {
        HubTrip t = lockMine(tripId);
        if (t.getStatus() != HubTripStatus.ACCEPTED) throw new AppException(ErrorCode.HUB_TRIP_INVALID_STATUS);
        if (!t.isConfirmed()) throw new AppException(ErrorCode.HUB_TRIP_NOT_CONFIRMED);
        if (t.getPickupDate() != null && LocalDate.now().isBefore(t.getPickupDate())) throw new AppException(ErrorCode.HUB_TRIP_TOO_EARLY);
        HubTripBox box = request == null || request.getOrderId() == null ? null
                : boxesOf(t).stream().filter(b -> b.getOrderId() == request.getOrderId()).findFirst().orElse(null);
        if (box == null) throw new AppException(ErrorCode.CAMPAIGN_INVALID_DATA);
        Set<String> keys = loadedKeys(t);
        keys.removeIf(k -> k.startsWith(box.getOrderId() + "-"));
        if (request.getLoaded() != null) {
            request.getLoaded().stream().filter(n -> n != null && n >= 1 && n <= box.getCrates()).forEach(n -> keys.add(box.getOrderId() + "-" + n));
        }
        t.setLoadedBoxes(keys.isEmpty() ? null : String.join(",", keys));
        t.setUpdatedAt(LocalDateTime.now());
        tripRepository.save(t);
        return toResponse(t);
    }

    /** Đã lấy hàng tại điểm tập kết, bắt đầu chở ra hub */
    @Transactional
    public HubTripResponse start(int tripId, String photoUrl) {
        HubTrip t = lockMine(tripId);
        if (t.getStatus() != HubTripStatus.ACCEPTED) throw new AppException(ErrorCode.HUB_TRIP_INVALID_STATUS);
        if (!t.isConfirmed()) throw new AppException(ErrorCode.HUB_TRIP_NOT_CONFIRMED);
        // Điểm tập kết còn mở thì hàng chưa được rời đi
        if (t.getPickupDate() != null && LocalDate.now().isBefore(t.getPickupDate())) {
            throw new AppException(ErrorCode.HUB_TRIP_TOO_EARLY);
        }
        // Nhận đủ từng thùng lên xe rồi mới chạy
        if (boxesOf(t).stream().anyMatch(b -> b.getLoaded().size() < b.getCrates())) {
            throw new AppException(ErrorCode.HUB_TRIP_BOXES_MISSING);
        }
        String photo = requirePhoto(photoUrl);
        LocalDateTime now = LocalDateTime.now();
        t.setLoadPhotoUrl(photo);
        t.setStatus(HubTripStatus.IN_TRANSIT);
        t.setStartedAt(now);
        t.setUpdatedAt(now);
        tripRepository.save(t);
        notifyAdmins("Xe chở hàng đã rời điểm tập kết", nameOf(t.getDriver()) + " đã lấy hàng Xe " + t.getTripNo() + " của chiến dịch \"" + t.getCampaign().getTitle() + "\", đang chở ra hub.", t.getCampaign().getCampaignId());
        return toResponse(t);
    }

    /** Đã giao hàng của một hub; giao xong hub cuối thì chuyến hoàn tất và tài xế nhận cước */
    @Transactional
    public HubTripResponse arriveStop(int tripId, int hubId, String photoUrl) {
        String photo = requirePhoto(photoUrl);
        HubTrip t = lockMine(tripId);
        if (t.getStatus() != HubTripStatus.IN_TRANSIT) throw new AppException(ErrorCode.HUB_TRIP_INVALID_STATUS);
        List<HubTripStop> stops = readStops(t.getStopsJson());
        HubTripStop stop = stops.stream().filter(s -> s.getHubId() != null && s.getHubId() == hubId && s.getArrivedAt() == null)
                .findFirst().orElseThrow(() -> new AppException(ErrorCode.HUB_TRIP_INVALID_STATUS));
        LocalDateTime now = LocalDateTime.now();
        stop.setArrivedAt(now.withNano(0).toString());
        stop.setPhotoUrl(photo);
        t.setStopsJson(writeStops(stops));
        t.setUpdatedAt(now);
        announceArrival(t, stop);
        if (stops.stream().allMatch(s -> s.getArrivedAt() != null)) finish(t, now);
        else tripRepository.save(t);
        return toResponse(t);
    }

    /** Ảnh làm bằng chứng khi hàng thiếu / hư giữa điểm tập kết, xe và hub */
    private static String requirePhoto(String url) {
        if (url == null || url.isBlank() || url.trim().length() > 500) throw new AppException(ErrorCode.HUB_PHOTO_REQUIRED);
        return url.trim();
    }

    private void finish(HubTrip t, LocalDateTime now) {
        t.setStatus(HubTripStatus.DELIVERED);
        t.setDeliveredAt(now);
        t.setUpdatedAt(now);
        // Trả cước chuyến cho tài xế từ ví sàn (chỉ một lần)
        if (t.getPaidAt() == null && t.getFeeAmount() != null) {
            BigDecimal net = escrowService.payHubTrip(t.getDriver(), "Xe " + t.getTripNo() + " chiến dịch " + t.getCampaign().getCode(), t.getFeeAmount(), serviceFeeRate);
            t.setPayoutAmount(net);
            t.setPaidAt(now);
            if (net.signum() > 0) {
                notify(t.getDriver(), "Đã nhận cước chuyến xe ra hub", "Bạn nhận " + net.setScale(0, RoundingMode.HALF_UP).toPlainString() + "đ cho Xe " + t.getTripNo() + " chiến dịch \"" + t.getCampaign().getTitle() + "\", đã cộng vào ví.", t.getCampaign().getCampaignId());
            }
        }
        tripRepository.save(t);
    }

    /** Xe tới một hub: nhắc admin xác nhận nhận hàng, báo người mua đơn đã tới nơi */
    private void announceArrival(HubTrip t, HubTripStop s) {
        int campaignId = t.getCampaign().getCampaignId();
        if (s.getOrderIds() != null) {
            for (PreOrder o : preOrderRepository.findAllById(s.getOrderIds())) {
                if (o.getStatus() != PreOrderStatus.READY || o.getBuyer() == null) continue;
                notify(o.getBuyer(), "Đơn đặt trước đã tới hub",
                        "Đơn " + o.getCode() + " đã tới hub " + s.getHubName() + " và đang chờ shipper giao tới bạn.", campaignId);
            }
        }
        notifyAdmins("Hàng tới hub, chờ xác nhận", nameOf(t.getDriver()) + " (Xe " + t.getTripNo() + ") đã giao hàng tới hub "
                + s.getHubName() + ". Mở Điểm trung chuyển để xác nhận đã nhận hàng.", campaignId);
    }

    /** Tài xế đã chở đơn này (để báo khi hub thấy thiếu / hư) */
    @Transactional(readOnly = true)
    public User driverOf(int campaignId, int orderId) {
        return tripRepository.findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(campaignId, HubTripStatus.CANCELLED).stream()
                .filter(t -> readStops(t.getStopsJson()).stream().anyMatch(s -> s.getOrderIds() != null && s.getOrderIds().contains(orderId)))
                .map(HubTrip::getDriver).filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    // ═════════════════════════════ nội bộ ═════════════════════════════

    /** Lưu vào đơn cước trả shipper giao chặng cuối, tính theo khoảng cách từ hub của chuyến tới địa chỉ người mua */
    private void setLastMilePay(Campaign c, List<HubTripSyncRequest.Trip> trips) {
        Map<Integer, HubTripStop> stopOfOrder = new LinkedHashMap<>();
        for (HubTripSyncRequest.Trip pt : trips) {
            if (pt.getStops() == null) continue;
            for (HubTripStop stop : pt.getStops()) {
                if (stop.getOrderIds() != null) stop.getOrderIds().forEach(id -> stopOfOrder.put(id, stop));
            }
        }
        if (stopOfOrder.isEmpty()) return;
        for (PreOrder o : preOrderRepository.findAllById(stopOfOrder.keySet())) {
            if (o.getCampaign().getCampaignId() != c.getCampaignId()
                    || CampaignShippingCalculator.PICKUP.equalsIgnoreCase(o.getFulfillmentMethod())) continue;
            HubTripStop stop = stopOfOrder.get(o.getPreOrderId());
            BigDecimal pay;
            if (stop.getHubLatitude() != null && stop.getHubLongitude() != null
                    && o.getShippingLatitude() != null && o.getShippingLongitude() != null) {
                double km = GeoUtils.haversineKm(stop.getHubLatitude(), stop.getHubLongitude(), o.getShippingLatitude(), o.getShippingLongitude())
                        * GeoUtils.ROAD_FACTOR;
                pay = shippingCalculator.baseFee(km);
            } else {
                pay = nz(o.getShippingBaseFee() != null ? o.getShippingBaseFee() : o.getShippingFee());
            }
            o.setHubDeliveryPay(pay);
            preOrderRepository.save(o);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Xe của tài xế phải chở được tổng kg của chuyến (và không nhỏ hơn mức tối thiểu cho chuyến ra hub) */
    private void requireCapacity(User driver, BigDecimal totalKg) {
        int capacity = VehicleType.capacityKgOf(driver.getVehicleType());
        if (capacity < feeCalculator.getMinVehicleKg() || (totalKg != null && capacity < totalKg.doubleValue())) {
            throw new AppException(ErrorCode.HUB_TRIP_VEHICLE_TOO_SMALL);
        }
    }

    private HubTrip lock(int tripId) {
        return tripRepository.findByIdForUpdate(tripId).orElseThrow(() -> new AppException(ErrorCode.HUB_TRIP_NOT_FOUND));
    }

    private HubTrip lockMine(int tripId) {
        HubTrip t = lock(tripId);
        if (t.getDriver() == null || t.getDriver().getId() != currentUserId()) throw new AppException(ErrorCode.HUB_TRIP_NOT_FOUND);
        return t;
    }

    private int currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.getName().matches("\\d+")) throw new AppException(ErrorCode.UNAUTHENTICATED);
        return Integer.parseInt(auth.getName());
    }

    private User findDriver(int userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getRole() != null && "SHIPPER".equalsIgnoreCase(u.getRole().getName()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.substring(0, Math.min(10, s.length())));
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Integer> declinedIds(HubTrip t) {
        String s = t.getDeclinedDriverIds();
        if (s == null || s.isBlank()) return new ArrayList<>();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> x.matches("\\d+")).map(Integer::valueOf).collect(Collectors.toList());
    }

    private static void setDeclined(HubTrip t, int driverId, boolean declined) {
        List<Integer> ids = declinedIds(t);
        ids.remove(Integer.valueOf(driverId));
        if (declined) ids.add(driverId);
        t.setDeclinedDriverIds(ids.isEmpty() ? null : ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    private static Set<Integer> hubIds(List<HubTripStop> stops) {
        return stops == null ? Set.of() : stops.stream().map(HubTripStop::getHubId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Set<String> loadedKeys(HubTrip t) {
        String s = t.getLoadedBoxes();
        return s == null || s.isBlank() ? new LinkedHashSet<>()
                : Arrays.stream(s.split(",")).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Thùng phải nhận lên xe: theo thứ tự hub trên lộ trình, mỗi đơn còn hiệu lực một dòng */
    private List<HubTripBox> boxesOf(HubTrip t) {
        List<HubTripStop> stops = readStops(t.getStopsJson());
        List<Integer> ids = stops.stream().filter(s -> s.getOrderIds() != null).flatMap(s -> s.getOrderIds().stream()).toList();
        if (ids.isEmpty()) return new ArrayList<>();
        Map<Integer, PreOrder> orders = preOrderRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(PreOrder::getPreOrderId, o -> o, (a, b) -> a));
        Set<String> keys = loadedKeys(t);
        List<HubTripBox> boxes = new ArrayList<>();
        for (HubTripStop s : stops) {
            if (s.getOrderIds() == null) continue;
            for (Integer id : s.getOrderIds()) {
                PreOrder o = orders.get(id);
                if (o == null || o.getStatus() == PreOrderStatus.CANCELLED || o.getCrateSize() == null) continue;
                List<Integer> loaded = new ArrayList<>();
                for (int n = 1; n <= o.getCrateQuantity(); n++) {
                    if (keys.contains(id + "-" + n)) loaded.add(n);
                }
                boxes.add(HubTripBox.builder().orderId(id).code(o.getCode()).recipientName(o.getRecipientName()).hubName(s.getHubName())
                        .crateKg(o.getCrateSize().kgDecimal().intValue()).crates(o.getCrateQuantity()).loaded(loaded)
                        .qrCode(t.getDriver() != null ? o.getQrCode() : null).build());
            }
        }
        return boxes;
    }

    private String writeStops(List<HubTripStop> stops) {
        try {
            return objectMapper.writeValueAsString(stops == null ? List.of() : stops);
        } catch (Exception e) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_DATA);
        }
    }

    private List<HubTripStop> readStops(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<HubTripStop>>() { });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String stopsText(List<HubTripStop> stops) {
        return stops.stream().map(s -> s.getHubName() + (s.getKg() != null ? " " + Math.round(s.getKg()) + " kg" : "")).collect(Collectors.joining(" → "));
    }

    private String tripText(Campaign c, HubTrip t) {
        return "Chiến dịch \"" + c.getTitle() + "\" Xe " + t.getTripNo() + ": nhận hàng tại "
                + (c.getCollectionSiteName() != null ? c.getCollectionSiteName() : "điểm tập kết")
                + (t.getPickupDate() != null ? " ngày " + t.getPickupDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "")
                + ", chở " + Math.round(t.getTotalKg().doubleValue()) + " kg ra " + stopsText(readStops(t.getStopsJson())) + ".";
    }

    private double netOf(BigDecimal gross) {
        if (gross == null) return 0;
        return gross.subtract(gross.multiply(serviceFeeRate).setScale(0, RoundingMode.HALF_UP)).doubleValue();
    }

    private static String nameOf(User u) {
        return u == null ? "Tài xế" : (u.getFullName() != null ? u.getFullName() : "Tài xế #" + u.getId());
    }

    private static String fit(String title) {
        return title.length() <= TITLE_MAX ? title : title.substring(0, TITLE_MAX - 1) + "…";
    }

    private void notify(User user, String title, String message, int campaignId) {
        try {
            notificationService.sendNotificationToUser(user, fit(title), message, NOTI_TYPE, campaignId);
        } catch (Exception e) {
            log.warn("[HubTrip] notify user #{} failed: {}", user.getId(), e.getMessage());
        }
    }

    private void notifyAdmins(String title, String message, int campaignId) {
        for (User admin : userRepository.findByRole_NameInAndStatus(List.of("ADMIN"), UserStatus.ACTIVE)) {
            notify(admin, title, message, campaignId);
        }
    }

    private HubTripResponse toResponse(HubTrip t) {
        List<HubTripBox> boxes = boxesOf(t);
        Campaign c = t.getCampaign();
        User d = t.getDriver();
        return HubTripResponse.builder()
                .tripId(t.getTripId())
                .campaignId(c.getCampaignId())
                .campaignTitle(c.getTitle())
                .tripNo(t.getTripNo())
                .pickupDate(t.getPickupDate())
                .totalKg(t.getTotalKg() == null ? 0 : t.getTotalKg().doubleValue())
                .status(t.getStatus().name())
                .confirmed(t.isConfirmed())
                .stops(readStops(t.getStopsJson()))
                .driverId(d != null ? d.getId() : null)
                .declinedDriverIds(declinedIds(t))
                .vehicleKg(t.getVehicleKg())
                .boxes(boxes)
                .boxTotal(boxes.stream().mapToInt(HubTripBox::getCrates).sum())
                .boxLoaded(boxes.stream().mapToInt(b -> b.getLoaded().size()).sum())
                .driverName(d != null ? d.getFullName() : null)
                .driverPhone(d != null ? d.getPhoneNumber() : null)
                .vehicleNumber(d != null ? d.getVehicleNumber() : null)
                .collectionSiteName(c.getCollectionSiteName())
                .collectionSiteAddress(c.getCollectionSiteAddress())
                .collectionSiteLatitude(c.getCollectionSiteLatitude())
                .collectionSiteLongitude(c.getCollectionSiteLongitude())
                .routeKm(t.getRouteKm())
                .feeAmount(t.getFeeAmount() == null ? 0 : t.getFeeAmount().doubleValue())
                .netAmount(netOf(t.getFeeAmount()))
                .serviceFeeRate(serviceFeeRate.doubleValue())
                .paidAt(t.getPaidAt())
                .acceptedAt(t.getAcceptedAt())
                .startedAt(t.getStartedAt())
                .loadPhotoUrl(t.getLoadPhotoUrl())
                .deliveredAt(t.getDeliveredAt())
                .build();
    }
}
