package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.campaign.*;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.campaign.CampaignResponse;
import com.sang.be_capnong.dto.response.campaign.FarmRegistrationResponse;
import com.sang.be_capnong.dto.response.campaign.PreOrderResponse;

import java.util.List;

/** Chiến dịch giải cứu nhiều nhà vườn (Review 2 - WF-01) */
public interface CampaignService {

    // ── Admin ──
    CampaignResponse createCampaign(CampaignUpsertRequest request);

    CampaignResponse updateCampaign(int campaignId, CampaignUpsertRequest request);

    /** Admin tuỳ chỉnh banner trang chủ của chiến dịch (được sửa ở mọi giai đoạn trừ khi đã huỷ / hoàn tất) */
    CampaignResponse updateBanner(int campaignId, com.sang.be_capnong.dto.request.campaign.CampaignBannerRequest request);

    PageResponse<CampaignResponse> getAdminCampaigns(String status, int page, int size);

    CampaignResponse getAdminCampaign(int campaignId);

    /** Đăng lời kêu gọi nhà vườn tham gia (gửi thông báo tới các Shop) */
    CampaignResponse postFarmCall(int campaignId);

    List<FarmRegistrationResponse> getRegistrations(int campaignId);

    FarmRegistrationResponse approveRegistration(int registrationId, CampaignNoteRequest request);

    FarmRegistrationResponse rejectRegistration(int registrationId, CampaignNoteRequest request);

    /** DRAFT -> ACTIVE */
    CampaignResponse publishCampaign(int campaignId);

    /** Huỷ thủ công (DRAFT / ACTIVE / THRESHOLD_MET) và hoàn toàn bộ cọc đã giữ */
    CampaignResponse cancelCampaign(int campaignId, CampaignNoteRequest request);

    /** THRESHOLD_MET -> FULFILLING */
    CampaignResponse startFulfilling(int campaignId);

    FarmRegistrationResponse recordIntake(int registrationId, CampaignIntakeRequest request);

    List<PreOrderResponse> getCampaignPreOrders(int campaignId);

    /**
     * Thiếu hàng sau khi cân: giữ hàng cho đơn cọc trước, đơn sau được giảm số thùng hoặc huỷ,
     * phần cọc tương ứng hoàn vào ví người mua.
     */
    CampaignResponse resolveShortfall(int campaignId);

    /** Mở kêu gọi nhà vườn bổ sung hàng cho phần thiếu (sau khi mọi nhà vườn đã giao) */
    CampaignResponse openTopUpCall(int campaignId);

    /** Đóng kêu gọi bổ sung */
    CampaignResponse closeTopUpCall(int campaignId);

    /** DEPOSIT_PAID -> PROCESSING: đóng gói, sinh mã QR, gán khu giao */
    PreOrderResponse packPreOrder(int preOrderId, PreOrderPackRequest request);

    /** Xe tới hub: các đơn PROCESSING -> READY (READY_FOR_PICKUP) */
    CampaignResponse dispatchToHub(int campaignId);

    /** Khách tự đến điểm tập kết đã nhận hàng và trả phần còn lại: PROCESSING -> DELIVERED */
    PreOrderResponse confirmPickup(int preOrderId);

    /** Đơn tự đến lấy đã đóng gói: gửi thông báo nhắc khách tới điểm tập kết */
    PreOrderResponse remindPickup(int preOrderId);

    /** Đã nhắc mà khách không tới: đóng đơn (FAILED), cọc không hoàn, để chiến dịch hoàn tất được */
    PreOrderResponse markPickupNoShow(int preOrderId, com.sang.be_capnong.dto.request.campaign.CampaignNoteRequest request);

    /** Bảng chia đơn giao: đơn giao tận nơi gom theo phường, xếp theo độ gấp */
    com.sang.be_capnong.dto.response.campaign.DispatchBoardResponse getDispatchBoard(int campaignId);

    /** Đưa các đơn đã đóng gói ra hub, giao thẳng cho shipper nếu có shipperId */
    com.sang.be_capnong.dto.response.campaign.DispatchBoardResponse dispatchPreOrders(int campaignId,
            com.sang.be_capnong.dto.request.campaign.PreOrderDispatchRequest request);

    /** Đơn giao thất bại: đưa về chờ chia lại để giao lần nữa */
    PreOrderResponse retryPreOrder(int preOrderId);

    /** Báo giá phí giao trước khi đặt (công khai) */
    com.sang.be_capnong.dto.response.campaign.CampaignShippingQuoteResponse quoteShipping(
            int campaignId, java.math.BigDecimal totalKg, String method, Double lat, Double lng, String address);

    /** FULFILLING -> COMPLETED: giải ngân cọc và chi trả nhà vườn */
    CampaignResponse completeCampaign(int campaignId);

    // ── Công khai / Người mua ──
    PageResponse<CampaignResponse> getActiveCampaigns(int page, int size);

    /**
     * Danh sách công khai theo giai đoạn: UPCOMING (sắp mở, đang gom nhà vườn), ONGOING (đang mở đặt trước),
     * DELIVERING (đã đủ ngưỡng / đang giao), DONE (đã hoàn thành).
     */
    PageResponse<CampaignResponse> getPublicCampaigns(String phase, int page, int size);

    CampaignResponse getPublicCampaign(int campaignId);

    PreOrderResponse createPreOrder(int campaignId, PreOrderCreateRequest request);

    PageResponse<PreOrderResponse> getMyPreOrders(int page, int size);

    PreOrderResponse getPreOrder(int preOrderId);

    /** Người mua quay lại từ trang PayOS: hỏi PayOS xem đã trả chưa */
    PreOrderResponse confirmDepositPayment(int preOrderId);

    /** Người mua huỷ khi chưa cọc */
    PreOrderResponse cancelPreOrder(int preOrderId);

    // ── Nhà vườn ──
    List<CampaignResponse> getOpenFarmCalls();

    FarmRegistrationResponse registerFarm(int campaignId, FarmRegistrationRequest request);

    List<FarmRegistrationResponse> getMyRegistrations();

    // ── Shipper ──
    List<PreOrderResponse> getReadyForPickup();

    List<PreOrderResponse> getMyShipperPreOrders();

    PreOrderResponse acceptPreOrder(int preOrderId);

    PreOrderResponse deliverPreOrder(int preOrderId, PreOrderDeliveryRequest request);

    PreOrderResponse failPreOrder(int preOrderId, PreOrderDeliveryRequest request);

    // ── Nền ──
    /** Webhook PayOS: trả về true nếu orderCode là tiền cọc chiến dịch (đã xử lý) */
    boolean handlePaidOrderCode(String orderCode);

    /** Đánh giá ngưỡng một chiến dịch ACTIVE đã qua hạn chốt */
    void evaluateCampaign(int campaignId);

    /** Huỷ một đơn PENDING quá hạn cọc */
    void expirePendingPreOrder(int preOrderId);
}
