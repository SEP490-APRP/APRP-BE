package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.dto.request.campaign.ShipperCallRequest;
import com.sang.be_capnong.dto.response.campaign.ShipperCallResult;
import com.sang.be_capnong.enums.VehicleType;
import com.sang.be_capnong.entity.Campaign;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.CampaignRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Admin kêu gọi xe (shipper) chở hàng chiến dịch từ điểm tập kết ra hub, và báo phân công cho từng tài xế */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShipperCallService {

    CampaignRepository campaignRepository;
    UserRepository userRepository;
    NotificationService notificationService;
    HubTripFeeCalculator feeCalculator;

    /** Cột notifications.title dài tối đa 50 ký tự */
    private static final int TITLE_MAX = 50;

    /**
     * Trả về số shipper đã được gửi thông báo.
     * Không bọc trong một transaction chung: mỗi thông báo tự commit riêng, một thông báo lỗi chỉ bị bỏ qua
     * (nếu bọc chung thì lỗi của một thông báo đánh dấu rollback cả lần gọi).
     */
    public ShipperCallResult callShippers(int campaignId, ShipperCallRequest request) {
        Campaign c = campaignRepository.findById(campaignId).orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));
        List<User> shippers = userRepository.findByRole_NameInAndStatus(List.of("SHIPPER"), UserStatus.ACTIVE);
        if (request != null && request.getShipperIds() != null && !request.getShipperIds().isEmpty()) {
            shippers = shippers.stream().filter(u -> request.getShipperIds().contains(u.getId())).toList();
        }
        boolean assign = request != null && Boolean.TRUE.equals(request.getAssignment());
        // Kêu gọi chung chỉ gửi cho tài xế có xe đủ tải (xe máy / xe nhỏ không nhận được chuyến chở hàng ra hub)
        int minKg = Math.max(feeCalculator.getMinVehicleKg(), request != null && request.getMinVehicleKg() != null ? request.getMinVehicleKg() : 0);
        int skipped = 0;
        if (!assign) {
            int before = shippers.size();
            shippers = shippers.stream().filter(u -> VehicleType.capacityKgOf(u.getVehicleType()) >= minKg).toList();
            skipped = before - shippers.size();
        }
        String date = formatDate(request != null ? request.getPickupDate() : null);
        int vehicles = request != null && request.getVehiclesNeeded() != null ? request.getVehiclesNeeded() : 0;
        boolean assignment = request != null && Boolean.TRUE.equals(request.getAssignment());
        String site = (c.getCollectionSiteName() != null ? c.getCollectionSiteName() : "điểm tập kết")
                + (c.getCollectionSiteAddress() != null ? " (" + c.getCollectionSiteAddress() + ")" : "");
        String title;
        StringBuilder body = new StringBuilder();
        if (assignment) {
            title = "Phân công xe chở hàng ra hub" + (date != null ? " ngày " + date : "");
            body.append("Chiến dịch \"").append(c.getTitle()).append("\": nhận hàng tại ").append(site);
            if (request.getVehicleKg() != null) body.append(", khoảng ").append(Math.round(request.getVehicleKg())).append(" kg");
            body.append(".");
        } else {
            title = "Cần " + (vehicles > 0 ? vehicles + " xe " : "xe ") + "chở hàng ra hub" + (date != null ? " ngày " + date : "");
            body.append("Chiến dịch \"").append(c.getTitle()).append("\" đã chốt đơn");
            if (request != null && request.getTotalKg() != null) body.append(", cần chở ").append(Math.round(request.getTotalKg())).append(" kg");
            if (vehicles > 0) body.append(" bằng khoảng ").append(vehicles).append(" xe");
            String hub = request != null ? request.getHubName() : null;
            body.append(" từ ").append(site).append(hub != null && !hub.isBlank() ? " ra " + hub.trim() : " ra các hub");
            if (date != null) body.append(" vào ngày ").append(date);
            body.append(". Số xe là dự kiến, CapNong xác nhận phân công sau khi cân hàng tại điểm tập kết.");
        }
        if (request != null && request.getMessage() != null && !request.getMessage().isBlank()) body.append(" ").append(request.getMessage().trim());
        int sent = 0;
        for (User s : shippers) {
            try {
                notificationService.sendNotificationToUser(s, fit(title), body.toString(), "CAMPAIGN_SHIPPER_CALL", campaignId);
                sent++;
            } catch (Exception e) {
                log.warn("[ShipperCall] notify shipper #{} failed: {}", s.getId(), e.getMessage());
            }
        }
        if (!assignment) {
            c.setShipperCalledAt(java.time.LocalDateTime.now());
            int planned = request != null && request.getTotalVehicles() != null ? request.getTotalVehicles() : vehicles;
            c.setShipperCallTrips(planned > 0 ? planned : null);
            campaignRepository.save(c);
        }
        return ShipperCallResult.builder().sent(sent).skippedSmallVehicle(skipped).minVehicleKg(minKg).build();
    }

    private static String fit(String title) {
        return title.length() <= TITLE_MAX ? title : title.substring(0, TITLE_MAX - 1) + "…";
    }

    private static String formatDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDate.parse(iso.substring(0, 10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }
}
