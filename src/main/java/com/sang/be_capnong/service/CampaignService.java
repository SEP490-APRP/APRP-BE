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

    /** Tự mở bán chiến dịch nháp khi tới publishAt; lỗi thì ghi lại và báo admin 1 lần. Trả true nếu đã đăng */
    boolean autoPublishCampaign(int campaignId);

    /** Tới hạn chốt góp hàng: nhà vườn góp (chờ duyệt + đã duyệt) chưa đủ ngưỡng tối thiểu thì tự huỷ chiến dịch. Trả true nếu đã huỷ */
    boolean evaluateFarmCutoff(int campaignId);

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

    /** Đơn sẽ bị giảm / huỷ nếu chốt phần thiếu */
    com.sang.be_capnong.dto.response.campaign.ShortfallPreviewResponse previewShortfall(int campaignId);

    /** Các đơn đã đóng gói để in tem; đơn chưa có mã tem an toàn thì được cấp mã */
    List<PreOrderResponse> preparePackageLabels(int campaignId);

    /** Thông tin công khai của thùng hàng khi quét tem QR */
    com.sang.be_capnong.dto.response.campaign.PackageTraceResponse tracePackage(String token);

    /** Mở kêu gọi nhà vườn bổ sung hàng cho phần thiếu (sau khi mọi nhà vườn đã giao) */
    CampaignResponse openTopUpCall(int campaignId);

    /** Đóng kêu gọi bổ sung */
    CampaignResponse closeTopUpCall(int campaignId);

    /** DEPOSIT_PAID -> PROCESSING: đóng gói, sinh mã QR, gán khu giao */
    PreOrderResponse packPreOrder(int preOrderId, PreOrderPackRequest request);


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

    /** Admin nhập hộ lô hàng cho shop đã đồng ý góp: tạo lô và duyệt luôn, báo cho shop */
    FarmRegistrationResponse adminAddRegistration(int campaignId, AdminFarmRegistrationRequest request);

    /**
     * Nhận thêm hàng khi nhà vườn giao thiếu: shop đã có lô thì cộng thẳng số kg vào lô đó,
     * shop chưa tham gia (hoặc shop hoàn toàn mới) thì tạo lô đã cân nhận.
     */
    FarmRegistrationResponse supplementSupply(int campaignId, com.sang.be_capnong.dto.request.campaign.AdminSupplementRequest request);

    /** Banner đầu trang chủ. publicOnly = true: chỉ chiến dịch người dùng xem được (bỏ nháp chưa kêu gọi, đã huỷ, đã xong) */
    com.sang.be_capnong.dto.response.campaign.HomeBannerConfigResponse getHomeBanners(boolean publicOnly);

    /** Admin lưu danh sách chiến dịch gắn banner đầu trang chủ + số giây tự chuyển */
    com.sang.be_capnong.dto.response.campaign.HomeBannerConfigResponse updateHomeBanners(HomeBannerConfigRequest request);

    List<FarmRegistrationResponse> getMyRegistrations();

    // ── Shipper ──
    List<PreOrderResponse> getReadyForPickup();

    List<PreOrderResponse> getMyShipperPreOrders();

    PreOrderResponse acceptPreOrder(int preOrderId);

    /** Shipper đã kiểm đủ thùng tại hub, lấy hàng và bắt đầu giao */
    PreOrderResponse pickupPreOrder(int preOrderId, com.sang.be_capnong.dto.request.campaign.PreOrderCrateRequest request);

    /** Shipper bỏ nhận đơn chưa lấy */
    PreOrderResponse releasePreOrder(int preOrderId);

    /** Hub xác nhận đã nhận đủ các đơn xe tải giao tới; trả về số đơn vừa xác nhận */
    int receiveAtHub(int hubId, com.sang.be_capnong.dto.request.campaign.PreOrderBatchRequest request);

    /** Hub báo đơn nhận bị thiếu / hư */
    PreOrderResponse reportHubIssue(int hubId, int preOrderId, com.sang.be_capnong.dto.request.hub.HubIssueRequest request);

    /** Đơn giao tận nơi gắn với một hub (đang tới, chờ shipper, đang giao, đã giao), kèm mã QR thùng */
    List<PreOrderResponse> getHubOrders(int hubId);

    /** Đánh dấu thùng đã đóng; đủ mọi thùng thì đơn đóng gói xong */
    PreOrderResponse markPackedCrates(int preOrderId, com.sang.be_capnong.dto.request.campaign.PreOrderCrateRequest request);

    List<PreOrderResponse> acceptPreOrderBatch(com.sang.be_capnong.dto.request.campaign.PreOrderBatchRequest request);

    com.sang.be_capnong.dto.response.campaign.ShipperCapacityResponse getShipperCapacity();

    /** Trả số shipper đã được nhắc */
    int notifyHubShippers(int campaignId, com.sang.be_capnong.dto.request.campaign.HubNotifyRequest request);

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
