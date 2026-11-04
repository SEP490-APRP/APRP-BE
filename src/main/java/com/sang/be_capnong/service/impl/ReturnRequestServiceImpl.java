package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.AdminReturnDecisionRequest;
import com.sang.be_capnong.dto.request.ReturnCreateRequest;
import com.sang.be_capnong.dto.request.ShipperReturnActionRequest;
import com.sang.be_capnong.dto.request.ShopReturnRespondRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ReturnRequestResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.MediaType;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.enums.ReturnStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ReturnRequestMapper;
import com.sang.be_capnong.repository.ImageRepository;
import com.sang.be_capnong.repository.OrderDetailRepository;
import com.sang.be_capnong.repository.OrderRepository;
import com.sang.be_capnong.repository.ReturnRequestRepository;
import com.sang.be_capnong.repository.WalletRepository;
import com.sang.be_capnong.repository.specification.ReturnRequestSpecification;
import org.springframework.data.jpa.domain.Specification;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.NotificationService;
import com.sang.be_capnong.service.ReturnRequestService;
import com.sang.be_capnong.service.WalletService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class ReturnRequestServiceImpl implements ReturnRequestService {

    ReturnRequestRepository returnRequestRepository;
    OrderDetailRepository orderDetailRepository;
    OrderRepository orderRepository;
    UserRepository userRepository;
    ImageRepository imageRepository;
    ReturnRequestMapper returnRequestMapper;
    NotificationService notificationService;
    WalletService walletService;
    WalletRepository walletRepository;

    public static final Set<ReturnStatus> TERMINAL_STATUSES = Set.of(
            ReturnStatus.COMPLETED,
            ReturnStatus.REJECTED,
            ReturnStatus.CANCELLED,
            ReturnStatus.RETURN_FAILED
    );

    public static boolean isReturnActive(ReturnStatus status) {
        return status != null && !TERMINAL_STATUSES.contains(status);
    }

    @Override
    @Transactional
    public ReturnRequestResponse createReturnRequest(ReturnCreateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

        Order order = orderDetail.getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 1. Kiểm tra quyền sở hữu đơn hàng
        if (order.getBuyer() == null || !Objects.equals(order.getBuyer().getId(), currentUserId)) {
            throw new AppException(ErrorCode.RETURN_UNAUTHORIZED);
        }

        // 2. Kiểm tra trạng thái đơn hàng (phải là DELIVERED hoặc COMPLETED)
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.ORDER_NOT_DELIVERED_YET);
        }

        // 3. Kiểm tra chính sách trả hàng 24 giờ
        LocalDateTime deliveryTime = order.getDeliveredAt();
        if (deliveryTime != null) {
            if (LocalDateTime.now().isAfter(deliveryTime.plusHours(24))) {
                throw new AppException(ErrorCode.RETURN_WINDOW_EXPIRED);
            }
        } else if (order.getEstimatedDeliveryDate() != null) {
            if (LocalDateTime.now().isAfter(order.getEstimatedDeliveryDate().plusDays(3))) {
                throw new AppException(ErrorCode.RETURN_WINDOW_EXPIRED);
            }
        }

        // 4. Kiểm tra xem sản phẩm này đã có yêu cầu hoàn trả chưa
        List<ReturnRequest> existingReturns = returnRequestRepository.findByOrderDetail_OrderDetailId(orderDetail.getOrderDetailId());
        boolean alreadyActiveOrDone = existingReturns.stream()
                .anyMatch(r -> isReturnActive(r.getStatus()) || r.getStatus() == ReturnStatus.COMPLETED);
        if (alreadyActiveOrDone || Boolean.TRUE.equals(orderDetail.getIsRequestedReturn())) {
            throw new AppException(ErrorCode.RETURN_ALREADY_REQUESTED);
        }

        // 5. Tính toán số tiền hoàn tối đa
        BigDecimal itemUnitPrice = orderDetail.getUnitPrice() != null ? orderDetail.getUnitPrice() : BigDecimal.ZERO;
        int itemQty = orderDetail.getQuantity() != null ? orderDetail.getQuantity() : 1;
        BigDecimal maxItemTotal = itemUnitPrice.multiply(BigDecimal.valueOf(itemQty));

        BigDecimal refundAmount = request.getRefundAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            refundAmount = maxItemTotal;
        } else if (refundAmount.compareTo(maxItemTotal) > 0) {
            throw new AppException(ErrorCode.RETURN_REFUND_AMOUNT_INVALID);
        }

        boolean isReturnGoods = request.getIsReturnGoods() == null || request.getIsReturnGoods();

        // 6. Tạo ReturnRequest
        User shop = order.getShop();
        ReturnRequest returnRequest = ReturnRequest.builder()
                .orderDetail(orderDetail)
                .shop(shop)
                .buyer(buyer)
                .reason(request.getReason())
                .status(ReturnStatus.PENDING)
                .refundAmount(refundAmount)
                .isReturnGoods(isReturnGoods)
                .returnShippingFee(BigDecimal.ZERO)
                .shippingPaymentMethod("WALLET")
                .isShippingFeePaid(false)
                .build();

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        // 7. Lưu bằng chứng (ảnh/video)
        if (request.getEvidenceUrls() != null && !request.getEvidenceUrls().isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (String url : request.getEvidenceUrls()) {
                if (url == null || url.trim().isEmpty()) continue;
                String lower = url.toLowerCase();
                MediaType mediaType = (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.contains("video"))
                        ? MediaType.VIDEO : MediaType.PHOTO;

                Image image = Image.builder()
                        .imageUrl(url.trim())
                        .mediaType(mediaType)
                        .createdAt(LocalDateTime.now())
                        .returnRequest(saved)
                        .build();
                images.add(image);
            }
            if (!images.isEmpty()) {
                imageRepository.saveAll(images);
                saved.setImages(images);
            }
        }

        // 8. Cập nhật OrderDetail & Order
        orderDetail.setIsRequestedReturn(true);
        orderDetailRepository.save(orderDetail);

        order.setStatus(OrderStatus.REFUND_REQUESTED);
        orderRepository.save(order);

        // 9. Gửi thông báo đến Shop
        if (shop != null) {
            String prodName = returnRequestMapper.mapProductName(orderDetail);
            notificationService.sendNotificationToUser(
                    shop,
                    "Yêu cầu hoàn trả sản phẩm mới",
                    "Khách hàng " + buyer.getFullName() + " đã yêu cầu hoàn trả cho sản phẩm \"" + prodName + "\" trong đơn hàng #" + order.getOrderId() + ".",
                    null
            );
        }

        log.info("[ReturnRequest] Created return #{} for orderDetail #{} by buyer #{}",
                saved.getReturnId(), orderDetail.getOrderDetailId(), buyer.getId());

        return returnRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ReturnRequestResponse shopRespond(int returnId, ShopReturnRespondRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequest returnRequest = findReturnRequestById(returnId);

        // Kiểm tra quyền Shop
        if (!Objects.equals(returnRequest.getShop().getId(), currentUserId) && !SecurityUtils.isAdmin()) {
            throw new AppException(ErrorCode.RETURN_UNAUTHORIZED);
        }

        if (returnRequest.getStatus() != ReturnStatus.PENDING && returnRequest.getStatus() != ReturnStatus.ADMIN_APPROVED) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        if (returnRequest.getStatus() == ReturnStatus.ADMIN_APPROVED && !request.isAccept()) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        Order order = returnRequest.getOrderDetail().getOrder();
        User buyer = returnRequest.getBuyer();
        String prodName = returnRequestMapper.mapProductName(returnRequest.getOrderDetail());

        if (request.isAccept()) {
            returnRequest.setShopResponse(request.getShopResponse() != null ? request.getShopResponse() : "Shop đồng ý yêu cầu hoàn trả.");

            if (request.getIsReturnGoods() != null) {
                returnRequest.setIsReturnGoods(request.getIsReturnGoods());
            }

            if (Boolean.FALSE.equals(returnRequest.getIsReturnGoods())) {
                // Shop chọn KHÔNG lấy lại hàng -> Hoàn tiền ngay cho người mua, không tốn ship hoàn
                returnRequest.setStatus(ReturnStatus.COMPLETED);
                returnRequest.setReturnShippingFee(BigDecimal.ZERO);
                processRefundSettlement(order, returnRequest);

                notificationService.sendNotificationToUser(
                        buyer,
                        "Shop đã đồng ý hoàn tiền",
                        "Shop " + returnRequest.getShop().getShopName() + " đã chấp thuận hoàn tiền không cần trả lại hàng cho sản phẩm \"" + prodName + "\".",
                        null
                );
            } else {
                // Shop chọn LẤY LẠI HÀNG -> Phí ship hoàn hoàn toàn do shop chịu 100%
                returnRequest.setIsReturnGoods(true);

                // 1. Xác định mức phí ship hoàn (từ request hoặc mặc định cước của đơn hàng)
                BigDecimal shipFee = request.getReturnShippingFee();
                if (shipFee == null || shipFee.compareTo(BigDecimal.ZERO) <= 0) {
                    shipFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.valueOf(20000);
                }
                returnRequest.setReturnShippingFee(shipFee);

                // 2. Xác định phương thức thanh toán tiền ship hoàn: WALLET hoặc COD
                String method = request.getShippingPaymentMethod();
                if (method == null || method.isBlank()) {
                    method = "WALLET";
                }
                method = method.trim().toUpperCase();
                if (!"WALLET".equals(method) && !"COD".equals(method)) {
                    method = "WALLET";
                }
                returnRequest.setShippingPaymentMethod(method);

                // 3. Nếu chọn WALLET: Kiểm tra số dư ví khả dụng của Shop và trừ cước hoàn
                if ("WALLET".equals(method)) {
                    walletService.deductShopReturnShippingFee(returnRequest);
                    returnRequest.setIsShippingFeePaid(true);
                } else {
                    // Nếu chọn COD: Shop sẽ đưa tiền mặt trực tiếp cho Shipper khi shipper giao hàng hoàn tới shop
                    returnRequest.setIsShippingFeePaid(false);
                }

                returnRequest.setStatus(ReturnStatus.RETURN_SHIPPING);

                notificationService.sendNotificationToUser(
                        buyer,
                        "Shop đã đồng ý yêu cầu trả hàng",
                        "Shop đã chấp thuận yêu cầu hoàn trả cho sản phẩm \"" + prodName + "\". Shipper sẽ được điều phối đến lấy hàng hoàn từ bạn (Người mua không phải trả phí ship).",
                        null
                );
            }
        } else {
            // Shop từ chối -> chuyển sang trạng thái tranh chấp DISPUTED để Admin phân xử
            returnRequest.setStatus(ReturnStatus.DISPUTED);
            returnRequest.setShopResponse(request.getShopResponse());

            notificationService.sendNotificationToUser(
                    buyer,
                    "Shop đã từ chối yêu cầu hoàn trả",
                    "Shop đã từ chối yêu cầu hoàn trả sản phẩm \"" + prodName + "\". Yêu cầu đã được chuyển lên Quản trị viên (Admin) để giải quyết.",
                    null
            );
        }

        ReturnRequest updated = returnRequestRepository.save(returnRequest);
        log.info("[ReturnRequest] Shop responded to return #{}: status={}, isReturnGoods={}, shipMethod={}",
                returnId, updated.getStatus(), updated.getIsReturnGoods(), updated.getShippingPaymentMethod());
        return returnRequestMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ReturnRequestResponse adminDecision(int returnId, AdminReturnDecisionRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ReturnRequest returnRequest = findReturnRequestById(returnId);
        Order order = returnRequest.getOrderDetail().getOrder();
        User buyer = returnRequest.getBuyer();
        User shop = returnRequest.getShop();
        String prodName = returnRequestMapper.mapProductName(returnRequest.getOrderDetail());

        returnRequest.setAdminRemark(request.getAdminRemark());

        if (request.isAccept()) {
            returnRequest.setStatus(ReturnStatus.ADMIN_APPROVED);

            if (request.getRefundAmount() != null && request.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
                returnRequest.setRefundAmount(request.getRefundAmount());
            }

            // Cách 1: Chuyển quyền cho Shop vào chọn phương án lấy lại hàng hoặc hoàn tiền ngay trong vòng 24h
            if (shop != null) {
                notificationService.sendNotificationToUser(
                        shop,
                        "Admin đã chấp thuận khiếu nại của khách",
                        "Admin đã chấp thuận khiếu nại hoàn tiền cho sản phẩm \"" + prodName + "\" trong đơn hàng #" + order.getOrderId() + ". Bạn có 24h để vào mục Khiếu nại chọn: Lấy lại hàng (chịu cước ship) hoặc Không lấy lại hàng (hoàn tiền ngay cho khách).",
                        null
                );
            }

            notificationService.sendNotificationToUser(
                    buyer,
                    "Admin đã chấp thuận khiếu nại của bạn",
                    "Admin đã chấp thuận khiếu nại cho sản phẩm \"" + prodName + "\". Đang chờ Shop lựa chọn phương án nhận lại hàng.",
                    null
            );

            log.info("[ReturnRequest] Admin approved dispute #{}. Transferred to Shop to choose return goods option.", returnId);
        } else {
            // Admin bác bỏ yêu cầu hoàn trả
            // Nếu trước đó shop đã trả tiền ship hoàn qua ví thì hoàn trả lại cho shop
            if (Boolean.TRUE.equals(returnRequest.getIsShippingFeePaid())) {
                walletService.refundShopReturnShippingFee(returnRequest);
            }

            returnRequest.setStatus(ReturnStatus.REJECTED);
            returnRequest.getOrderDetail().setIsRequestedReturn(false);
            orderDetailRepository.save(returnRequest.getOrderDetail());

            // Phục hồi trạng thái đơn nếu không còn yêu cầu hoàn trả nào khác
            restoreOrderStateIfNoActiveReturns(order);

            notificationService.sendNotificationToUser(
                    buyer,
                    "Admin đã từ chối khiếu nại",
                    "Admin đã bác bỏ yêu cầu khiếu nại hoàn trả sản phẩm \"" + prodName + "\". Lý do: " + request.getAdminRemark(),
                    null
            );
        }

        ReturnRequest updated = returnRequestRepository.save(returnRequest);
        log.info("[ReturnRequest] Admin decided return #{}: status={}", returnId, updated.getStatus());
        return returnRequestMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ReturnRequestResponse shipperAccept(int returnId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User shipper = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ReturnRequest returnRequest = findReturnRequestById(returnId);

        if (returnRequest.getStatus() != ReturnStatus.RETURN_SHIPPING) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        if (returnRequest.getShipper() != null && !Objects.equals(returnRequest.getShipper().getId(), currentUserId)) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        // Kiểm tra số dư ví khả dụng của Shipper nếu đơn hoàn trả bằng hình thức COD
        if ("COD".equalsIgnoreCase(returnRequest.getShippingPaymentMethod())) {
            BigDecimal returnShipFee = returnRequest.getReturnShippingFee() != null
                    ? returnRequest.getReturnShippingFee() : BigDecimal.valueOf(20000);
            BigDecimal requiredCommission = returnShipFee.multiply(BigDecimal.valueOf(0.03));
            if (requiredCommission.signum() > 0) {
                Wallet shipperWallet = walletRepository.findFirstByUser_IdAndType(currentUserId, "SHIPPER").orElse(null);
                if (shipperWallet == null || (shipperWallet.getTotalBalance() != null && shipperWallet.getTotalBalance().compareTo(requiredCommission) < 0)) {
                    log.warn("[ReturnRequest] Shipper #{} insufficient balance to accept COD return #{}: required={}",
                            currentUserId, returnId, requiredCommission);
                    throw new AppException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
                }
            }
        }

        returnRequest.setShipper(shipper);
        ReturnRequest updated = returnRequestRepository.save(returnRequest);

        notificationService.sendNotificationToUser(
                returnRequest.getBuyer(),
                "Shipper đã nhận đơn hoàn hàng",
                "Shipper " + shipper.getFullName() + " đã tiếp nhận đơn hoàn trả cho đơn hàng #" + returnRequest.getOrderDetail().getOrder().getOrderId() + ".",
                null
        );

        log.info("[ReturnRequest] Shipper #{} accepted return #{}", currentUserId, returnId);
        return returnRequestMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ReturnRequestResponse shipperPickup(int returnId, ShipperReturnActionRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequest returnRequest = findReturnRequestById(returnId);

        if (returnRequest.getShipper() == null || !Objects.equals(returnRequest.getShipper().getId(), currentUserId)) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ASSIGNED_TO_RETURN);
        }

        if (returnRequest.getStatus() != ReturnStatus.RETURN_SHIPPING) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        returnRequest.setStatus(ReturnStatus.RETURN_PICKED_UP);
        returnRequest.setPickedUpAt(LocalDateTime.now());
        if (request != null && request.getProofImageUrl() != null && !request.getProofImageUrl().isBlank()) {
            saveShipperProofImage(returnRequest, request.getProofImageUrl());
        }
        ReturnRequest updated = returnRequestRepository.save(returnRequest);

        String prodName = returnRequestMapper.mapProductName(returnRequest.getOrderDetail());
        notificationService.sendNotificationToUser(
                returnRequest.getBuyer(),
                "Hàng hoàn đã được shipper lấy",
                "Shipper đã nhận sản phẩm hoàn \"" + prodName + "\" từ bạn và đang chuyển về shop.",
                null
        );

        if (returnRequest.getShop() != null) {
            notificationService.sendNotificationToUser(
                    returnRequest.getShop(),
                    "Shipper đang vận chuyển hàng hoàn về",
                    "Shipper đã lấy sản phẩm hoàn \"" + prodName + "\" từ người mua và đang trên đường giao về shop.",
                    null
            );
        }

        log.info("[ReturnRequest] Shipper #{} picked up return #{}", currentUserId, returnId);
        return returnRequestMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ReturnRequestResponse shipperDeliver(int returnId, ShipperReturnActionRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequest returnRequest = findReturnRequestById(returnId);

        if (returnRequest.getShipper() == null || !Objects.equals(returnRequest.getShipper().getId(), currentUserId)) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ASSIGNED_TO_RETURN);
        }

        if (returnRequest.getStatus() != ReturnStatus.RETURN_PICKED_UP) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        returnRequest.setStatus(ReturnStatus.RETURN_DELIVERED);
        returnRequest.setReturnedAt(LocalDateTime.now());
        if (request != null && request.getProofImageUrl() != null && !request.getProofImageUrl().isBlank()) {
            saveShipperProofImage(returnRequest, request.getProofImageUrl());
        }

        // Sau khi giao hàng hoàn về cho shop -> hoàn tất quy trình
        returnRequest.setStatus(ReturnStatus.COMPLETED);

        Order order = returnRequest.getOrderDetail().getOrder();
        processRefundSettlement(order, returnRequest);

        ReturnRequest updated = returnRequestRepository.save(returnRequest);

        String prodName = returnRequestMapper.mapProductName(returnRequest.getOrderDetail());
        notificationService.sendNotificationToUser(
                returnRequest.getBuyer(),
                "Hoàn tất quy trình hoàn trả hàng",
                "Sản phẩm hoàn \"" + prodName + "\" đã được giao về cho shop thành công. Tiền hoàn đã được hoàn tất xử lý!",
                null
        );

        if (returnRequest.getShop() != null) {
            notificationService.sendNotificationToUser(
                    returnRequest.getShop(),
                    "Đã nhận lại hàng hoàn trả",
                    "Shipper đã giao thành công sản phẩm hoàn \"" + prodName + "\" về cho gian hàng của bạn.",
                    null
            );
        }

        log.info("[ReturnRequest] Shipper #{} delivered return #{} -> COMPLETED", currentUserId, returnId);
        return returnRequestMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ReturnRequestResponse shipperFail(int returnId, ShipperReturnActionRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequest returnRequest = findReturnRequestById(returnId);

        if (returnRequest.getShipper() == null || !Objects.equals(returnRequest.getShipper().getId(), currentUserId)) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ASSIGNED_TO_RETURN);
        }

        if (request != null && request.getProofImageUrl() != null && !request.getProofImageUrl().isBlank()) {
            saveShipperProofImage(returnRequest, request.getProofImageUrl());
        }
        String note = request != null && request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote().trim() : "Giao nhận hàng hoàn không thành công";

        // Giai đoạn 1: Chưa lấy được hàng từ Người mua (RETURN_SHIPPING) -> Huỷ trả hàng, hoàn lại cước cho Shop
        if (returnRequest.getStatus() == ReturnStatus.RETURN_SHIPPING) {
            returnRequest.setStatus(ReturnStatus.RETURN_FAILED);

            // Hoàn lại cước ship cho Shop nếu đã trừ qua ví
            if (Boolean.TRUE.equals(returnRequest.getIsShippingFeePaid())) {
                walletService.refundShopReturnShippingFee(returnRequest);
            }

            returnRequest.getOrderDetail().setIsRequestedReturn(false);
            orderDetailRepository.save(returnRequest.getOrderDetail());

            Order order = returnRequest.getOrderDetail().getOrder();
            restoreOrderStateIfNoActiveReturns(order);

            notificationService.sendNotificationToUser(
                    returnRequest.getBuyer(),
                    "Lấy hàng hoàn không thành công",
                    "Shipper không thể lấy hàng hoàn từ bạn. Lý do: " + note + ". Yêu cầu hoàn hàng đã bị đóng.",
                    null
            );
            if (returnRequest.getShop() != null) {
                notificationService.sendNotificationToUser(
                        returnRequest.getShop(),
                        "Lấy hàng hoàn thất bại",
                        "Shipper không thể lấy hàng hoàn từ người mua. Lý do: " + note + ". Cước ship hoàn (nếu đã trừ ví) đã được hoàn lại cho bạn.",
                        null
                );
            }
        } else if (returnRequest.getStatus() == ReturnStatus.RETURN_PICKED_UP) {
            // Giai đoạn 2: Đã lấy hàng từ Người mua nhưng giao về Shop thất bại -> Chuyển sang DISPUTED để Admin can thiệp
            returnRequest.setStatus(ReturnStatus.DISPUTED);
            returnRequest.setAdminRemark("Shipper báo giao về Shop thất bại: " + note + ". Hàng đang ở Shipper, cần Admin kiểm tra xử lý.");

            notificationService.sendNotificationToUser(
                    returnRequest.getShop(),
                    "Giao hàng hoàn về shop thất bại",
                    "Shipper đã nhận hàng từ người mua nhưng chưa thể giao về gian hàng của bạn. Lý do: " + note + ". Yêu cầu đã chuyển lên Admin.",
                    null
            );
        } else {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        ReturnRequest updated = returnRequestRepository.save(returnRequest);
        log.info("[ReturnRequest] Shipper #{} reported fail for return #{}: status={}, note={}",
                currentUserId, returnId, updated.getStatus(), note);
        return returnRequestMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public ReturnRequestResponse cancelReturnRequest(int returnId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        ReturnRequest returnRequest = findReturnRequestById(returnId);

        if (!Objects.equals(returnRequest.getBuyer().getId(), currentUserId)) {
            throw new AppException(ErrorCode.RETURN_UNAUTHORIZED);
        }

        if (returnRequest.getStatus() != ReturnStatus.PENDING && returnRequest.getStatus() != ReturnStatus.DISPUTED) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        // Nếu shop đã trả phí ship hoàn qua ví, hoàn lại tiền vào ví cho shop
        if (Boolean.TRUE.equals(returnRequest.getIsShippingFeePaid())) {
            walletService.refundShopReturnShippingFee(returnRequest);
        }

        returnRequest.setStatus(ReturnStatus.CANCELLED);
        returnRequest.getOrderDetail().setIsRequestedReturn(false);
        orderDetailRepository.save(returnRequest.getOrderDetail());

        Order order = returnRequest.getOrderDetail().getOrder();
        restoreOrderStateIfNoActiveReturns(order);

        ReturnRequest updated = returnRequestRepository.save(returnRequest);
        log.info("[ReturnRequest] Buyer #{} cancelled return #{}", currentUserId, returnId);
        return returnRequestMapper.toResponse(updated);
    }

    @Override
    public ReturnRequestResponse getReturnRequestById(int returnId) {
        ReturnRequest returnRequest = findReturnRequestById(returnId);
        validateAccess(returnRequest);
        return returnRequestMapper.toResponse(returnRequest);
    }

    @Override
    public PageResponse<ReturnRequestResponse> getMyBuyerReturns(int page, int size, ReturnStatus status) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReturnRequest> resultPage;
        if (status != null) {
            resultPage = returnRequestRepository.findByBuyer_IdAndStatus(currentUserId, status, pageable);
        } else {
            resultPage = returnRequestRepository.findByBuyer_Id(currentUserId, pageable);
        }
        return PageResponse.from(resultPage, returnRequestMapper.toResponseList(resultPage.getContent()));
    }

    @Override
    public PageResponse<ReturnRequestResponse> getMyShopReturns(int page, int size, ReturnStatus status) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReturnRequest> resultPage;
        if (status != null) {
            resultPage = returnRequestRepository.findByShop_IdAndStatus(currentUserId, status, pageable);
        } else {
            resultPage = returnRequestRepository.findByShop_Id(currentUserId, pageable);
        }
        return PageResponse.from(resultPage, returnRequestMapper.toResponseList(resultPage.getContent()));
    }

    @Override
    public PageResponse<ReturnRequestResponse> getMyShipperReturns(int page, int size, ReturnStatus status) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReturnRequest> resultPage;
        if (status != null) {
            resultPage = returnRequestRepository.findByShipper_IdAndStatus(currentUserId, status, pageable);
        } else {
            resultPage = returnRequestRepository.findByShipper_Id(currentUserId, pageable);
        }
        return PageResponse.from(resultPage, returnRequestMapper.toResponseList(resultPage.getContent()));
    }

    @Override
    public PageResponse<ReturnRequestResponse> getAvailableReturnOrdersForShipper(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ReturnRequest> resultPage = returnRequestRepository.findByStatusAndShipperIsNull(ReturnStatus.RETURN_SHIPPING, pageable);
        return PageResponse.from(resultPage, returnRequestMapper.toResponseList(resultPage.getContent()));
    }

    @Override
    public PageResponse<ReturnRequestResponse> getAllDisputesForAdmin(int page, int size, ReturnStatus status, String keyword) {
        if (!SecurityUtils.isAdmin()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<ReturnRequest> spec = ReturnRequestSpecification.filterDisputes(status, keyword);
        Page<ReturnRequest> resultPage = returnRequestRepository.findAll(spec, pageable);
        return PageResponse.from(resultPage, returnRequestMapper.toResponseList(resultPage.getContent()));
    }

    @Override
    public List<ReturnRequestResponse> getAllDisputesList() {
        if (!SecurityUtils.isAdmin()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // Trả về danh sách khiếu nại (DISPUTED, PENDING, RETURN_SHIPPING...) cho admin
        List<ReturnRequest> all = returnRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return returnRequestMapper.toResponseList(all);
    }

    @Override
    public boolean autoCheckPayout(int orderCode) {
        log.info("[ReturnRequest] Auto-check payout called for orderCode={}", orderCode);
        return true;
    }

    @Override
    public ReturnRequestResponse checkPayoutStatus(int returnId) {
        ReturnRequest returnRequest = findReturnRequestById(returnId);
        return returnRequestMapper.toResponse(returnRequest);
    }

    @Override
    @Transactional
    public void autoResolveAdminApprovedReturn(int returnId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId).orElse(null);
        if (returnRequest == null || returnRequest.getStatus() != ReturnStatus.ADMIN_APPROVED) {
            return;
        }
        Order order = returnRequest.getOrderDetail().getOrder();
        returnRequest.setIsReturnGoods(false);
        returnRequest.setReturnShippingFee(BigDecimal.ZERO);
        returnRequest.setStatus(ReturnStatus.COMPLETED);
        returnRequest.setShopResponse("Quá 24 giờ Shop không chọn phương án thu hồi hàng, hệ thống tự động hoàn tiền cho người mua (khách không cần gửi lại hàng).");
        processRefundSettlement(order, returnRequest);
        returnRequestRepository.save(returnRequest);

        String prodName = returnRequestMapper.mapProductName(returnRequest.getOrderDetail());
        notificationService.sendNotificationToUser(
                returnRequest.getBuyer(),
                "Hoàn tiền tự động thành công",
                "Yêu cầu hoàn trả cho sản phẩm \"" + prodName + "\" đã được hoàn tiền tự động vào ví của bạn (Shop không chọn nhận lại hàng sau 24h).",
                null
        );
        if (returnRequest.getShop() != null) {
            notificationService.sendNotificationToUser(
                    returnRequest.getShop(),
                    "Yêu cầu hoàn trả đã tự động hoàn tất",
                    "Do quá 24h bạn chưa chọn phương án thu hồi hàng cho đơn #" + order.getOrderId() + ", hệ thống đã tự động hoàn tiền cho người mua.",
                    null
            );
        }
        log.info("[ReturnRequest] Auto-resolved return #{} after 24h in ADMIN_APPROVED status", returnId);
    }


    // ==========================================
    // HELPER METHODS
    // ==========================================

    private ReturnRequest findReturnRequestById(int returnId) {
        return returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
    }

    private void validateAccess(ReturnRequest request) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        boolean isBuyer = request.getBuyer() != null && Objects.equals(request.getBuyer().getId(), currentUserId);
        boolean isShop = request.getShop() != null && Objects.equals(request.getShop().getId(), currentUserId);
        boolean isShipper = request.getShipper() != null && Objects.equals(request.getShipper().getId(), currentUserId);

        if (!isBuyer && !isShop && !isShipper) {
            throw new AppException(ErrorCode.RETURN_UNAUTHORIZED);
        }
    }

    private void processRefundSettlement(Order order, ReturnRequest returnRequest) {
        if (order == null) return;
        BigDecimal currentRefund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal additionalRefund = returnRequest.getRefundAmount() != null ? returnRequest.getRefundAmount() : BigDecimal.ZERO;
        order.setRefundAmount(currentRefund.add(additionalRefund));

        List<OrderDetail> allDetails = order.getOrderDetails() != null ? order.getOrderDetails() : Collections.emptyList();
        List<ReturnRequest> allReturns = returnRequestRepository.findByOrderDetail_Order_OrderId(order.getOrderId());

        Set<Integer> completedDetailIds = allReturns.stream()
                .filter(r -> r.getStatus() == ReturnStatus.COMPLETED || r.getReturnId() == returnRequest.getReturnId())
                .map(r -> r.getOrderDetail().getOrderDetailId())
                .collect(Collectors.toSet());
        boolean allItemsCompletedRefund = !allDetails.isEmpty() && allDetails.stream()
                .allMatch(d -> completedDetailIds.contains(d.getOrderDetailId()));

        if (allItemsCompletedRefund) {
            order.setStatus(OrderStatus.REFUNDED);
        } else {
            // Đơn hoàn 1 phần: kiểm tra xem còn yêu cầu hoàn nào khác đang mở không
            boolean hasRemainingActive = allReturns.stream()
                    .filter(r -> r.getReturnId() != returnRequest.getReturnId())
                    .anyMatch(r -> isReturnActive(r.getStatus()));
            if (!hasRemainingActive) {
                LocalDateTime deliveredAt = order.getDeliveredAt();
                boolean isExpired24h = deliveredAt != null && LocalDateTime.now().isAfter(deliveredAt.plusHours(24));
                if (order.getCompletedAt() != null || isExpired24h) {
                    order.setStatus(OrderStatus.COMPLETED);
                } else {
                    order.setStatus(OrderStatus.DELIVERED);
                }
            }
        }
        orderRepository.save(order);

        // Bút toán ví: hoàn tiền về ví cho người mua, trừ tiền tạm giữ của Shop / Sàn
        try {
            walletService.processReturnRefund(returnRequest);
        } catch (Exception e) {
            log.error("[ReturnRequest] Failed to process wallet refund for return #{}: {}", returnRequest.getReturnId(), e.getMessage(), e);
            throw e;
        }

        log.info("[ReturnRequest] Refund settled for order #{}: added {} đ, new total refund={}, orderStatus={}",
                order.getOrderId(), additionalRefund, order.getRefundAmount(), order.getStatus());
    }

    private void restoreOrderStateIfNoActiveReturns(Order order) {
        if (order == null) return;
        List<ReturnRequest> allReturns = returnRequestRepository.findByOrderDetail_Order_OrderId(order.getOrderId());
        boolean hasActive = allReturns.stream()
                .anyMatch(r -> isReturnActive(r.getStatus()));

        if (!hasActive && order.getStatus() == OrderStatus.REFUND_REQUESTED) {
            LocalDateTime deliveredAt = order.getDeliveredAt();
            boolean isExpired24h = deliveredAt != null && LocalDateTime.now().isAfter(deliveredAt.plusHours(24));
            if (order.getCompletedAt() != null || isExpired24h) {
                order.setStatus(OrderStatus.COMPLETED);
            } else {
                order.setStatus(OrderStatus.DELIVERED);
            }
            orderRepository.save(order);
            log.info("[ReturnRequest] Restored order #{} status to {}", order.getOrderId(), order.getStatus());
        }
    }

    private void saveShipperProofImage(ReturnRequest returnRequest, String proofImageUrl) {
        if (proofImageUrl == null || proofImageUrl.isBlank()) return;
        try {
            Image image = Image.builder()
                    .imageUrl(proofImageUrl.trim())
                    .mediaType(MediaType.PHOTO)
                    .createdAt(LocalDateTime.now())
                    .returnRequest(returnRequest)
                    .build();
            imageRepository.save(image);
        } catch (Exception e) {
            log.warn("[ReturnRequest] Failed to save shipper proof image: {}", e.getMessage());
        }
    }
}
