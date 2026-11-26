package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.dto.request.hub.DeliveryHubRequest;
import com.sang.be_capnong.dto.response.hub.DeliveryHubResponse;
import com.sang.be_capnong.entity.DeliveryHub;
import com.sang.be_capnong.entity.PreOrder;
import com.sang.be_capnong.enums.PreOrderStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.DeliveryHubRepository;
import com.sang.be_capnong.repository.PreOrderRepository;
import com.sang.be_capnong.util.GeoUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Điểm trung chuyển: admin quản lý, hệ thống gán hub gần người mua nhất khi đưa đơn ra hub */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryHubService {

    DeliveryHubRepository hubRepository;
    PreOrderRepository preOrderRepository;
    HubTripService hubTripService;

    @Transactional(readOnly = true)
    public List<DeliveryHubResponse> list(boolean activeOnly) {
        List<DeliveryHub> hubs = activeOnly ? hubRepository.findByActiveTrueOrderByNameAsc() : hubRepository.findAllByOrderByActiveDescNameAsc();
        return hubs.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DeliveryHubResponse create(DeliveryHubRequest request) {
        DeliveryHub hub = new DeliveryHub();
        apply(hub, request);
        hub.setCreatedAt(LocalDateTime.now());
        return toResponse(hubRepository.save(hub));
    }

    @Transactional
    public DeliveryHubResponse update(int hubId, DeliveryHubRequest request) {
        DeliveryHub hub = find(hubId);
        apply(hub, request);
        return toResponse(hubRepository.save(hub));
    }

    /** Ngưng dùng hub: không xoá vì đơn cũ còn trỏ tới */
    @Transactional
    public DeliveryHubResponse deactivate(int hubId) {
        DeliveryHub hub = find(hubId);
        hub.setActive(false);
        hub.setUpdatedAt(LocalDateTime.now());
        return toResponse(hubRepository.save(hub));
    }

    public DeliveryHub find(int hubId) {
        return hubRepository.findById(hubId).orElseThrow(() -> new AppException(ErrorCode.HUB_NOT_FOUND));
    }

    /** Hub đang hoạt động gần toạ độ nhất; không có toạ độ hoặc chưa có hub thì trả null */
    public DeliveryHub nearest(Double lat, Double lng) {
        if (lat == null || lng == null) return null;
        return hubRepository.findByActiveTrueOrderByNameAsc().stream()
                .min(Comparator.comparingDouble(h -> GeoUtils.haversineKm(lat, lng, h.getLatitude(), h.getLongitude())))
                .orElse(null);
    }

    private void apply(DeliveryHub hub, DeliveryHubRequest r) {
        if (r == null || isBlank(r.getName()) || isBlank(r.getAddress()) || r.getLatitude() == null || r.getLongitude() == null
                || Math.abs(r.getLatitude()) > 90 || Math.abs(r.getLongitude()) > 180) {
            throw new AppException(ErrorCode.HUB_INVALID);
        }
        hub.setName(r.getName().trim());
        hub.setAddress(r.getAddress().trim());
        hub.setLatitude(r.getLatitude());
        hub.setLongitude(r.getLongitude());
        hub.setPhone(trimOrNull(r.getPhone()));
        hub.setOpenHours(trimOrNull(r.getOpenHours()));
        hub.setNote(trimOrNull(r.getNote()));
        if (r.getActive() != null) hub.setActive(r.getActive());
        hub.setUpdatedAt(LocalDateTime.now());
    }

    /** Đơn giao tận nơi xe tải đã đưa tới hub, shipper chưa lấy đi */
    private List<PreOrder> arrivedOrders(int hubId) {
        Map<Integer, Set<Integer>> onTheWay = new HashMap<>();
        return preOrderRepository.findByHub_HubIdAndStatusInOrderByReadyAtDescPreOrderIdAsc(hubId, List.of(PreOrderStatus.READY)).stream()
                .filter(p -> !CampaignShippingCalculator.PICKUP.equalsIgnoreCase(p.getFulfillmentMethod()) && p.getPickedUpAt() == null)
                .filter(p -> !onTheWay.computeIfAbsent(p.getCampaign().getCampaignId(), hubTripService::orderIdsOnTheWay)
                        .contains(p.getPreOrderId()))
                .toList();
    }

    private DeliveryHubResponse toResponse(DeliveryHub h) {
        List<PreOrder> arrived = arrivedOrders(h.getHubId());
        return DeliveryHubResponse.builder()
                .hubId(h.getHubId())
                .name(h.getName())
                .address(h.getAddress())
                .latitude(h.getLatitude())
                .longitude(h.getLongitude())
                .phone(h.getPhone())
                .openHours(h.getOpenHours())
                .note(h.getNote())
                .active(h.isActive())
                .waitingOrders(arrived.stream().filter(p -> p.getHubReceivedAt() != null && p.getShipper() == null).count())
                .unconfirmedOrders(arrived.stream().filter(p -> p.getHubReceivedAt() == null).count())
                .createdAt(h.getCreatedAt())
                .build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
