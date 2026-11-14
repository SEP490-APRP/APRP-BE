package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.CheckoutPreviewRequest;
import com.sang.be_capnong.dto.request.CreateOrderRequest;
import com.sang.be_capnong.dto.request.OrderItemRequest;
import com.sang.be_capnong.dto.response.CheckoutPreviewResponse;
import com.sang.be_capnong.dto.response.CheckoutPreviewResponse.ShopOrderPreview;
import com.sang.be_capnong.dto.response.OrderDetailResponse;
import com.sang.be_capnong.dto.response.OrderResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.enums.ReturnStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.OrderMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.GoogleMapService;
import com.sang.be_capnong.service.NotificationService;
import com.sang.be_capnong.service.OrderService;
import com.sang.be_capnong.service.ShippingFeeService;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.enums.VoucherScope;
import com.sang.be_capnong.enums.VoucherTarget;
import com.sang.be_capnong.enums.VoucherType;
import com.sang.be_capnong.dto.request.ShippingFeeRequest;
import com.sang.be_capnong.dto.response.ShippingFeeResponse;
import java.util.Objects;
import java.util.Comparator;
import com.sang.be_capnong.dto.request.DeliverOrderRequest;
import com.sang.be_capnong.dto.request.FailOrderRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sang.be_capnong.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    OrderDetailRepository orderDetailRepository;
    ProductRepository productRepository;
    MysteryBoxRepository mysteryBoxRepository;
    BuildComboRepository buildComboRepository;
    ProductMysteryRepository productMysteryRepository;
    ProductComboRepository productComboRepository;
    UserRepository userRepository;
    ReturnRequestRepository returnRequestRepository;
    OrderMapper orderMapper;
    ShippingFeeService shippingFeeService;
    VoucherRepository voucherRepository;
    VoucherUserRepository voucherUserRepository;
    VoucherUsageRepository voucherUsageRepository;
    CartItemRepository cartItemRepository;
    CheckoutBatchRepository checkoutBatchRepository;
    SavedAddressRepository savedAddressRepository;
    GoogleMapService googleMapService;
    com.sang.be_capnong.service.CartService cartService;
    com.sang.be_capnong.service.WalletService walletService;
    com.sang.be_capnong.service.PaymentService paymentService;
    com.sang.be_capnong.service.DispatchService dispatchService;
    com.sang.be_capnong.service.ShipperService shipperService;
    com.sang.be_capnong.service.ReferralService referralService;
    NotificationService notificationService;

    /** Bán kính tối đa (km) để shipper thấy đơn READY trong danh sách */
    static final double READY_LIST_MAX_KM = 15.0;

    // ============================================================
    //  TẠO ĐƠN HÀNG
    // ============================================================

    @Override
    public CheckoutPreviewResponse previewCheckout(CheckoutPreviewRequest request) {
        User buyer = getCurrentUser();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        java.util.Map<User, List<OrderDetail>> mapShopToDetails = groupItemsByShop(request.getItems(), false);
        return buildCheckoutPreview(buyer, request.getShippingAddress(), request.getVoucherCodes(), mapShopToDetails);
    }

    @Transactional
    @Override
    public List<OrderResponse> createOrder(CreateOrderRequest request) {
        User buyer = getCurrentUser();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }

        // 1. Nhóm items và validate (trừ tồn kho)
        java.util.Map<User, List<OrderDetail>> mapShopToDetails = groupItemsByShop(request.getItems(), true);

        // 2. Tính toán tiền và Voucher giống như Preview
        CheckoutPreviewResponse previewRes = buildCheckoutPreview(buyer, request.getShippingAddress(), request.getVoucherCodes(), mapShopToDetails);

        OrderStatus initialStatus = resolveInitialStatus(request.getPaymentMethod());
        List<OrderResponse> responses = new ArrayList<>();
        List<Order> savedOrders = new ArrayList<>();

        CheckoutBatch batch = null;
        if (previewRes.getShopOrders().size() > 1 || "PAYOS".equalsIgnoreCase(request.getPaymentMethod()) || "TRANSFER".equalsIgnoreCase(request.getPaymentMethod())) {
            batch = CheckoutBatch.builder()
                    .buyer(buyer)
                    .paymentMethod(request.getPaymentMethod().toUpperCase())
                    .status(BatchStatus.PENDING)
                    .totalAmount(previewRes.getFinalTotalAmount())
                    .batchDiscountAmount(previewRes.getTotalVoucherDiscount())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            batch = checkoutBatchRepository.save(batch);
        }

        // 3. Tạo Order dựa trên preview
        for (ShopOrderPreview sp : previewRes.getShopOrders()) {
            User shop = userRepository.findById(sp.getShopId()).orElseThrow();
            List<OrderDetail> details = mapShopToDetails.get(shop);

            Order order = Order.builder()
                    .buyer(buyer)
                    .shop(shop)
                    .checkoutBatch(batch)
                    .recipientName(request.getRecipientName() != null ? request.getRecipientName() : buyer.getFullName())
                    .recipientPhone(request.getRecipientPhone() != null ? request.getRecipientPhone() : buyer.getPhoneNumber())
                    .shippingAddress(request.getShippingAddress())
                    .note(request.getNote())
                    .shippingFee(sp.getShippingFee())
                    .paymentMethod(request.getPaymentMethod().toUpperCase())
                    .status(initialStatus)
                    .totalAmount(sp.getTotalAmount())
                    .createdAt(LocalDateTime.now())
                    // KHONG gán voucherDiscountAmount vào Order entity
                    .build();

            Order savedOrder = orderRepository.save(order);
            savedOrders.add(savedOrder);
            for (OrderDetail d : details) {
                d.setOrder(savedOrder);
            }
            orderDetailRepository.saveAll(details);

            // Ghi nhận hoa hồng tiếp thị liên kết (nếu có attribution hợp lệ)
            referralService.processOrderReferral(savedOrder, details, sp.getVoucherDiscount());
            
            OrderResponse res = buildOrderResponse(savedOrder, details);
            res.setVoucherDiscountAmount(sp.getVoucherDiscount());
            res.setBatchId(batch != null ? batch.getId() : null);
            res.setShopId(shop.getId());
            responses.add(res);
        }

        // Tự động cập nhật thông tin cá nhân của buyer nếu chưa có
        boolean needUpdateBuyer = false;
        if ((buyer.getAddress() == null || buyer.getAddress().isBlank()) && request.getShippingAddress() != null && !request.getShippingAddress().isBlank()) {
            buyer.setAddress(request.getShippingAddress());
            needUpdateBuyer = true;
        }
        if ((buyer.getPhoneNumber() == null || buyer.getPhoneNumber().isBlank()) && request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank()) {
            buyer.setPhoneNumber(request.getRecipientPhone());
            needUpdateBuyer = true;
        }
        if ((buyer.getFullName() == null || buyer.getFullName().isBlank()) && request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
            buyer.setFullName(request.getRecipientName());
            needUpdateBuyer = true;
        }
        if (needUpdateBuyer) {
            userRepository.save(buyer);
        }

        // Tự động lưu vào Sổ địa chỉ (SavedAddress) nếu người dùng chọn hoặc nếu là đơn đầu tiên
        long addressCount = savedAddressRepository.countByUser_IdAndIsActiveTrue(buyer.getId());
        boolean shouldSaveAddress = Boolean.TRUE.equals(request.getSaveAddressToBook()) || (addressCount == 0);
        if (shouldSaveAddress && addressCount < 10 && request.getShippingAddress() != null && !request.getShippingAddress().isBlank()) {
            String targetAddress = request.getShippingAddress().trim();
            boolean alreadyExists = savedAddressRepository.findByUser_IdAndIsActiveTrue(buyer.getId()).stream()
                    .anyMatch(a -> a.getAddress().equalsIgnoreCase(targetAddress));
            if (!alreadyExists) {
                Double lat = null;
                Double lng = null;
                try {
                    double[] coords = googleMapService.geocodeAddress(targetAddress);
                    if (coords != null && coords.length == 2) {
                        lat = coords[0];
                        lng = coords[1];
                    }
                } catch (Exception e) {
                    log.warn("Geocoding address on order creation failed: {}", e.getMessage());
                }

                String label = request.getAddressLabel() != null && !request.getAddressLabel().isBlank()
                        ? request.getAddressLabel().trim()
                        : (addressCount == 0 ? "Nhà (Mặc định)" : "Địa chỉ mới");

                SavedAddress newSavedAddress = SavedAddress.builder()
                        .user(buyer)
                        .label(label)
                        .recipientName(request.getRecipientName() != null && !request.getRecipientName().isBlank() ? request.getRecipientName().trim() : (buyer.getFullName() != null ? buyer.getFullName() : "Người nhận"))
                        .phone(request.getRecipientPhone() != null && !request.getRecipientPhone().isBlank() ? request.getRecipientPhone().trim() : (buyer.getPhoneNumber() != null ? buyer.getPhoneNumber() : ""))
                        .address(targetAddress)
                        .latitude(lat)
                        .longitude(lng)
                        .isDefault(addressCount == 0)
                        .isActive(true)
                        .build();
                savedAddressRepository.save(newSavedAddress);
                log.info("Auto-saved delivery address to address book for user {}: {}", buyer.getId(), targetAddress);
            }
        }

        // 4. Cập nhật lịch sử dùng Voucher (Lưu discountAmount vào VoucherUsage)
        if (request.getVoucherCodes() != null && !request.getVoucherCodes().isEmpty()) {
            for (String code : request.getVoucherCodes()) {
                Voucher v = voucherRepository.findByVoucherCode(code.toUpperCase()).orElse(null);
                if (v != null) {
                    VoucherUser voucherUser = voucherUserRepository.findByUser_IdAndVoucher_VoucherId(buyer.getId(), v.getVoucherId()).orElse(null);
                    if (voucherUser == null) {
                        continue; // Người mua chưa thu thập/không sở hữu voucher này
                    }
                    int qty = voucherUser.getQuantity() == null ? 0 : voucherUser.getQuantity();
                    int used = voucherUser.getQuantityUsed() == null ? 0 : voucherUser.getQuantityUsed();
                    if (qty <= used) {
                        continue;
                    }

                    // Tính tổng discount thực tế mà voucher này đã mang lại trong đơn hàng
                    BigDecimal totalDiscountFromThisVoucher = BigDecimal.ZERO;
                    for (int i = 0; i < savedOrders.size(); i++) {
                        CheckoutPreviewResponse.ShopOrderPreview sp = previewRes.getShopOrders().get(i);
                        if (sp.getVoucherDiscountsDetail() != null && sp.getVoucherDiscountsDetail().containsKey(code.toUpperCase())) {
                            BigDecimal d = sp.getVoucherDiscountsDetail().get(code.toUpperCase());
                            if (d != null && d.compareTo(BigDecimal.ZERO) > 0) {
                                totalDiscountFromThisVoucher = totalDiscountFromThisVoucher.add(d);
                            }
                        }
                    }

                    // CHỈ trừ lượt dùng và lưu VoucherUsage nếu voucher này thực sự giảm giá cho ít nhất 1 shop trong đơn
                    if (totalDiscountFromThisVoucher.compareTo(BigDecimal.ZERO) > 0) {
                        v.setUsedCount((v.getUsedCount() == null ? 0 : v.getUsedCount()) + 1);
                        voucherRepository.save(v);
                           
                        voucherUser.setQuantityUsed(used + 1);
                        voucherUserRepository.save(voucherUser);
                                
                        for (int i = 0; i < savedOrders.size(); i++) {
                            Order savedOrder = savedOrders.get(i);
                            CheckoutPreviewResponse.ShopOrderPreview sp = previewRes.getShopOrders().get(i);
                            if (voucherAppliesToOrderShop(v, savedOrder)) {
                                BigDecimal discountForThisOrder = sp.getVoucherDiscountsDetail() != null ? sp.getVoucherDiscountsDetail().get(code.toUpperCase()) : null;
                                if (discountForThisOrder != null && discountForThisOrder.compareTo(BigDecimal.ZERO) > 0) {
                                    VoucherUsage vu = VoucherUsage.builder()
                                        .order(savedOrder)
                                        .user(buyer)
                                        .voucher(v)
                                        .usedAt(LocalDateTime.now())
                                        .discountAmount(discountForThisOrder)
                                        .build();
                                    voucherUsageRepository.save(vu);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Dọn dẹp giỏ hàng — CHỈ xoá những món đã được tick mua VÀ thuộc về buyer hiện tại
        //    (Chống lỗ hổng IDOR xóa nhầm/xóa trộm giỏ hàng của người khác khi Checkout)
        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            List<CartItem> itemsToDelete = cartItemRepository.findAllById(request.getCartItemIds())
                    .stream()
                    .filter(item -> item.getCart() != null
                            && item.getCart().getUser() != null
                            && Objects.equals(item.getCart().getUser().getId(), buyer.getId()))
                    .toList();

            if (!itemsToDelete.isEmpty()) {
                cartItemRepository.deleteAll(itemsToDelete);
            }
        }

        // 6. Gửi thông báo cho Shop và Buyer
        for (Order savedOrder : savedOrders) {
            try {
                if (savedOrder.getShop() != null) {
                    notificationService.sendNotificationToUser(
                            savedOrder.getShop(),
                            "Bạn có đơn hàng mới!",
                            "Đơn hàng #" + savedOrder.getOrderId() + " đã được đặt và đang chờ xác nhận.",
                            "NEW_ORDER",
                            savedOrder.getOrderId()
                    );
                }
                notificationService.sendNotificationToUser(
                        buyer,
                        "Đặt hàng thành công",
                        "Đơn hàng #" + savedOrder.getOrderId() + " đã được tạo thành công.",
                        "ORDER_STATUS",
                        savedOrder.getOrderId()
                );
            } catch (Exception e) {
                log.warn("[Order] Failed to send order creation notification for order #{}: {}", savedOrder.getOrderId(), e.getMessage());
            }
        }

        return responses;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StockShortage {
        String itemType; // "PRODUCT", "MYSTERY_BOX", "BUILD_COMBO"
        Integer id;
        String itemName;
        String imageUrl;
        int requestedQuantity;
        int availableQuantity;
    }

    private java.util.Map<User, List<OrderDetail>> groupItemsByShop(List<OrderItemRequest> items, boolean deductStock) {
        // BƯỚC 1: Thu thập tổng số lượng requested theo từng món và kiểm tra toàn bộ danh sách
        java.util.Map<Integer, Integer> productQtyMap = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Integer> boxQtyMap = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Integer> comboQtyMap = new java.util.LinkedHashMap<>();

        for (OrderItemRequest itemReq : items) {
            validateItemHasExactlyOneId(itemReq);
            int qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 0;
            if (itemReq.getProductId() != null) {
                productQtyMap.merge(itemReq.getProductId(), qty, Integer::sum);
            } else if (itemReq.getMysteryBoxId() != null) {
                boxQtyMap.merge(itemReq.getMysteryBoxId(), qty, Integer::sum);
            } else if (itemReq.getBuildComboId() != null) {
                comboQtyMap.merge(itemReq.getBuildComboId(), qty, Integer::sum);
            }
        }

        List<StockShortage> shortages = new ArrayList<>();

        // 1. Kiểm tra tồn kho của tất cả Product
        for (java.util.Map.Entry<Integer, Integer> entry : productQtyMap.entrySet()) {
            Integer prodId = entry.getKey();
            int requested = entry.getValue();
            Product product = productRepository.findById(prodId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            User productShop = product.getShop();
            if (productShop == null || productShop.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
            int available = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (available < requested) {
                String img = (product.getImages() != null && !product.getImages().isEmpty())
                        ? product.getImages().get(0).getImageUrl()
                        : null;
                shortages.add(StockShortage.builder()
                        .itemType("PRODUCT")
                        .id(product.getProductId())
                        .itemName(product.getProductName())
                        .imageUrl(img)
                        .requestedQuantity(requested)
                        .availableQuantity(available)
                        .build());
            }
        }

        // 2. Kiểm tra tồn kho của tất cả Mystery Box
        for (java.util.Map.Entry<Integer, Integer> entry : boxQtyMap.entrySet()) {
            Integer boxId = entry.getKey();
            int requested = entry.getValue();
            MysteryBox box = mysteryBoxRepository.findById(boxId)
                    .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));
            if (!Boolean.TRUE.equals(box.getIsActive())) throw new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND);
            User boxShop = box.getShop();
            if (boxShop == null || boxShop.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
            int sold = box.getSoldQuantity() == null ? 0 : box.getSoldQuantity();
            int remaining = Math.max(0, (box.getTotalQuantity() == null ? 0 : box.getTotalQuantity()) - sold);
            if (remaining < requested) {
                shortages.add(StockShortage.builder()
                        .itemType("MYSTERY_BOX")
                        .id(box.getMysteryId())
                        .itemName(box.getBoxType())
                        .imageUrl(box.getImageUrl())
                        .requestedQuantity(requested)
                        .availableQuantity(remaining)
                        .build());
            }
        }

        // 3. Kiểm tra tồn kho của tất cả Build Combo
        for (java.util.Map.Entry<Integer, Integer> entry : comboQtyMap.entrySet()) {
            Integer comboId = entry.getKey();
            int requested = entry.getValue();
            BuildCombo combo = buildComboRepository.findById(comboId)
                    .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
            if (!"ACTIVE".equalsIgnoreCase(combo.getStatus())) throw new AppException(ErrorCode.COMBO_NOT_FOUND);
            User comboShop = combo.getShop();
            if (comboShop == null || comboShop.getStatus() != UserStatus.ACTIVE) {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
            int sold = combo.getSoldQuantity() == null ? 0 : combo.getSoldQuantity();
            int remaining = Math.max(0, (combo.getTotalQuantity() == null ? 0 : combo.getTotalQuantity()) - sold);
            if (remaining < requested) {
                shortages.add(StockShortage.builder()
                        .itemType("BUILD_COMBO")
                        .id(combo.getComboId())
                        .itemName(combo.getComboName())
                        .imageUrl(combo.getImageUrl())
                        .requestedQuantity(requested)
                        .availableQuantity(remaining)
                        .build());
            }
        }

        // Nếu có bất kỳ mặt hàng nào thiếu kho, thông báo TỔNG HỢP toàn bộ danh sách thiếu
        if (!shortages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (shortages.size() == 1) {
                StockShortage s = shortages.get(0);
                String typeLabel = switch (s.getItemType()) {
                    case "MYSTERY_BOX" -> "Hộp bí ẩn";
                    case "BUILD_COMBO" -> "Gói Combo";
                    default -> "Sản phẩm";
                };
                sb.append(typeLabel).append(" \"").append(s.getItemName())
                  .append("\" trong kho chỉ còn ").append(s.getAvailableQuantity())
                  .append(" (bạn đặt ").append(s.getRequestedQuantity()).append("). Vui lòng cập nhật lại giỏ hàng!");
            } else {
                sb.append("Có ").append(shortages.size()).append(" mặt hàng không đủ số lượng trong kho:\n");
                for (StockShortage s : shortages) {
                    String tag = switch (s.getItemType()) {
                        case "MYSTERY_BOX" -> "[Hộp bí ẩn]";
                        case "BUILD_COMBO" -> "[Gói Combo]";
                        default -> "[Sản phẩm]";
                    };
                    sb.append("• ").append(tag).append(" \"").append(s.getItemName())
                      .append("\": Kho còn ").append(s.getAvailableQuantity())
                      .append(", bạn đặt ").append(s.getRequestedQuantity()).append("\n");
                }
                sb.append("Vui lòng cập nhật lại số lượng trong giỏ hàng!");
            }
            throw new AppException(ErrorCode.STOCK_INSUFFICIENT, sb.toString().trim(), shortages);
        }

        // BƯỚC 2: Thực hiện trừ kho nguyên tử (Atomic Conditional Update) và đóng gói OrderDetail
        java.util.Map<User, List<OrderDetail>> mapShopToDetails = new java.util.HashMap<>();
        List<StockShortage> raceShortages = new ArrayList<>();

        for (OrderItemRequest itemReq : items) {
            User shopOwner;
            OrderDetail detail;

            if (itemReq.getProductId() != null) {
                Product product = productRepository.findById(itemReq.getProductId()).orElseThrow();
                if (deductStock) {
                    int updated = productRepository.decreaseStockAtomic(product.getProductId(), itemReq.getQuantity());
                    if (updated == 0) {
                        String img = (product.getImages() != null && !product.getImages().isEmpty())
                                ? product.getImages().get(0).getImageUrl()
                                : null;
                        raceShortages.add(StockShortage.builder()
                                .itemType("PRODUCT")
                                .id(product.getProductId())
                                .itemName(product.getProductName())
                                .imageUrl(img)
                                .requestedQuantity(itemReq.getQuantity())
                                .availableQuantity(product.getStockQuantity() != null ? product.getStockQuantity() : 0)
                                .build());
                    } else {
                        product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
                    }
                }
                shopOwner = product.getShop();
                detail = OrderDetail.builder()
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(resolveProductPrice(product))
                        .itemType("PRODUCT")
                        .isRequestedReturn(false)
                        .build();
            } else if (itemReq.getMysteryBoxId() != null) {
                MysteryBox box = mysteryBoxRepository.findById(itemReq.getMysteryBoxId()).orElseThrow();
                if (deductStock) {
                    int updated = mysteryBoxRepository.decreaseStockAtomic(box.getMysteryId(), itemReq.getQuantity());
                    if (updated == 0) {
                        int sold = box.getSoldQuantity() == null ? 0 : box.getSoldQuantity();
                        int remaining = Math.max(0, (box.getTotalQuantity() == null ? 0 : box.getTotalQuantity()) - sold);
                        raceShortages.add(StockShortage.builder()
                                .itemType("MYSTERY_BOX")
                                .id(box.getMysteryId())
                                .itemName(box.getBoxType())
                                .imageUrl(box.getImageUrl())
                                .requestedQuantity(itemReq.getQuantity())
                                .availableQuantity(remaining)
                                .build());
                    } else {
                        box.setSoldQuantity((box.getSoldQuantity() == null ? 0 : box.getSoldQuantity()) + itemReq.getQuantity());
                    }
                }
                shopOwner = box.getShop();
                detail = OrderDetail.builder()
                        .mysteryBox(box)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(box.getPrice())
                        .itemType("MYSTERY_BOX")
                        .isRequestedReturn(false)
                        .build();
            } else {
                BuildCombo combo = buildComboRepository.findById(itemReq.getBuildComboId()).orElseThrow();
                if (deductStock) {
                    int updated = buildComboRepository.decreaseStockAtomic(combo.getComboId(), itemReq.getQuantity());
                    if (updated == 0) {
                        int sold = combo.getSoldQuantity() == null ? 0 : combo.getSoldQuantity();
                        int remaining = Math.max(0, (combo.getTotalQuantity() == null ? 0 : combo.getTotalQuantity()) - sold);
                        raceShortages.add(StockShortage.builder()
                                .itemType("BUILD_COMBO")
                                .id(combo.getComboId())
                                .itemName(combo.getComboName())
                                .imageUrl(combo.getImageUrl())
                                .requestedQuantity(itemReq.getQuantity())
                                .availableQuantity(remaining)
                                .build());
                    } else {
                        combo.setSoldQuantity((combo.getSoldQuantity() == null ? 0 : combo.getSoldQuantity()) + itemReq.getQuantity());
                    }
                }
                shopOwner = combo.getShop();
                detail = OrderDetail.builder()
                        .buildCombo(combo)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(combo.getPrice())
                        .itemType("BUILD_COMBO")
                        .isRequestedReturn(false)
                        .build();
            }
            mapShopToDetails.computeIfAbsent(shopOwner, k -> new ArrayList<>()).add(detail);
        }

        if (!raceShortages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Các mặt hàng sau vừa có khách hàng khác đặt mua trước, hiện không còn đủ số lượng:\n");
            for (StockShortage s : raceShortages) {
                String tag = switch (s.getItemType()) {
                    case "MYSTERY_BOX" -> "[Hộp bí ẩn]";
                    case "BUILD_COMBO" -> "[Gói Combo]";
                    default -> "[Sản phẩm]";
                };
                sb.append("• ").append(tag).append(" \"").append(s.getItemName()).append("\"\n");
            }
            sb.append("Vui lòng cập nhật lại giỏ hàng!");
            throw new AppException(ErrorCode.STOCK_INSUFFICIENT, sb.toString().trim(), raceShortages);
        }

        return mapShopToDetails;
    }

    private CheckoutPreviewResponse buildCheckoutPreview(User buyer, String shippingAddress, List<String> voucherCodes, java.util.Map<User, List<OrderDetail>> mapShopToDetails) {
        BigDecimal totalGoodsAmount = BigDecimal.ZERO;
        BigDecimal totalShippingFee = BigDecimal.ZERO;
        List<ShopOrderPreview> shopOrderPreviews = new ArrayList<>();
        
        for (java.util.Map.Entry<User, List<OrderDetail>> entry : mapShopToDetails.entrySet()) {
            User shop = entry.getKey();
            List<OrderDetail> details = entry.getValue();

            BigDecimal shopGoodsAmount = details.stream()
                    .map(d -> d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, java.math.RoundingMode.HALF_UP);

            BigDecimal shippingFee;
            try {
                ShippingFeeRequest feeReq = new ShippingFeeRequest();
                feeReq.setShopId(shop.getId());
                feeReq.setBuyerAddress(shippingAddress);
                ShippingFeeResponse feeRes = shippingFeeService.calculateShippingFee(feeReq);
                shippingFee = (feeRes != null && feeRes.getShippingFee() != null) ? feeRes.getShippingFee() : BigDecimal.valueOf(15_000);
            } catch (Exception e) {
                shippingFee = BigDecimal.valueOf(15_000);
            }

            totalGoodsAmount = totalGoodsAmount.add(shopGoodsAmount);
            totalShippingFee = totalShippingFee.add(shippingFee);
            
            // Build item responses cho preview
            List<OrderDetailResponse> itemResponses = new ArrayList<>();
            for(OrderDetail d : details) {
                String name = "";
                String imageUrl = "";
                if ("PRODUCT".equals(d.getItemType()) && d.getProduct() != null) {
                    name = d.getProduct().getProductName();
                    imageUrl = d.getProduct().getImages() != null && !d.getProduct().getImages().isEmpty() ? d.getProduct().getImages().get(0).getImageUrl() : "";
                } else if ("MYSTERY_BOX".equals(d.getItemType()) && d.getMysteryBox() != null) {
                    name = d.getMysteryBox().getBoxType();
                    imageUrl = d.getMysteryBox().getImageUrl() != null ? d.getMysteryBox().getImageUrl() : "";
                } else if ("BUILD_COMBO".equals(d.getItemType()) && d.getBuildCombo() != null) {
                    name = d.getBuildCombo().getComboName();
                    imageUrl = d.getBuildCombo().getImageUrl() != null ? d.getBuildCombo().getImageUrl() : "";
                }
                itemResponses.add(OrderDetailResponse.builder()
                        .itemType(d.getItemType())
                        .itemName(name)
                        .imageUrl(imageUrl)
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .subtotal(d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                        .build());
            }

            ShopOrderPreview shopPreview = ShopOrderPreview.builder()
                .shopId(shop.getId())
                .shopName(shop.getShopName() != null ? shop.getShopName() : shop.getFullName())
                .goodsAmount(shopGoodsAmount)
                .shippingFee(shippingFee)
                .voucherDiscount(BigDecimal.ZERO)
                .items(itemResponses)
                .voucherDiscountsDetail(new java.util.HashMap<>())
                .build();
                
            shopOrderPreviews.add(shopPreview);
        }

        BigDecimal totalVoucherDiscount = applyVouchersAndReturnTotalDiscount(buyer, voucherCodes, shopOrderPreviews, totalGoodsAmount, totalShippingFee);

        for (ShopOrderPreview sp : shopOrderPreviews) {
            BigDecimal shopTotal = sp.getGoodsAmount().add(sp.getShippingFee()).subtract(sp.getVoucherDiscount()).setScale(0, java.math.RoundingMode.HALF_UP);
            if (shopTotal.compareTo(BigDecimal.ZERO) < 0) shopTotal = BigDecimal.ZERO;
            sp.setTotalAmount(shopTotal);
        }

        BigDecimal finalTotalAmount = shopOrderPreviews.stream()
                .map(ShopOrderPreview::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, java.math.RoundingMode.HALF_UP);

        BigDecimal effectiveTotalDiscount = totalGoodsAmount.add(totalShippingFee).subtract(finalTotalAmount).setScale(0, java.math.RoundingMode.HALF_UP);
        if (effectiveTotalDiscount.compareTo(BigDecimal.ZERO) < 0) effectiveTotalDiscount = BigDecimal.ZERO;

        return CheckoutPreviewResponse.builder()
                .shopOrders(shopOrderPreviews)
                .totalGoodsAmount(totalGoodsAmount)
                .totalShippingFee(totalShippingFee)
                .totalVoucherDiscount(effectiveTotalDiscount)
                .finalTotalAmount(finalTotalAmount)
                .build();
    }

    private BigDecimal applyVouchersAndReturnTotalDiscount(User buyer, List<String> voucherCodes, List<ShopOrderPreview> shopPreviews, BigDecimal totalGoodsAmount, BigDecimal totalShippingFee) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (voucherCodes == null || voucherCodes.isEmpty() || shopPreviews == null || shopPreviews.isEmpty()) {
            return totalDiscount;
        }

        // Bước 0: Phân loại danh sách Voucher hợp lệ theo cấp độ và mục tiêu (loại trừ mã trùng lặp)
        List<Voucher> shopVouchers = new ArrayList<>();
        List<Voucher> platformOrderVouchers = new ArrayList<>();
        List<Voucher> platformShippingVouchers = new ArrayList<>();

        List<String> distinctCodes = voucherCodes.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        for (String code : distinctCodes) {
            Voucher voucher = voucherRepository.findByVoucherCode(code).orElse(null);
            if (voucher == null || !Boolean.TRUE.equals(voucher.getActive()) || (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now()))) {
                continue;
            }

            if (voucher.getUsageLimit() != null) {
                int used = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
                if (used >= voucher.getUsageLimit()) {
                    continue;
                }
            }

            // Kiểm tra quyền sở hữu trong ví VoucherUser của người mua
            if (buyer != null) {
                VoucherUser vu = voucherUserRepository.findByUser_IdAndVoucher_VoucherId(buyer.getId(), voucher.getVoucherId()).orElse(null);
                if (vu == null) {
                    continue; // Người mua chưa thu thập/không sở hữu voucher này
                }
                int qty = vu.getQuantity() == null ? 0 : vu.getQuantity();
                int used = vu.getQuantityUsed() == null ? 0 : vu.getQuantityUsed();
                if (qty <= used) {
                    continue; // Người mua đã sử dụng hết số lượt sở hữu
                }
            }

            if (voucher.getPerUserLimit() != null && buyer != null) {
                int userUsed = voucherUsageRepository.countByUserAndVoucher(buyer, voucher);
                if (userUsed >= voucher.getPerUserLimit()) {
                    continue;
                }
            }

            User creator = voucher.getShop();
            String creatorRole = (creator != null && creator.getRole() != null) ? creator.getRole().getName() : "SHOP";
            boolean isCreatorAdmin = "ADMIN".equalsIgnoreCase(creatorRole);

            VoucherScope scope = voucher.getVoucherScope();
            if (!isCreatorAdmin) {
                // Voucher do Shop tạo TUYỆT ĐỐI chỉ có phạm vi SHOP, không bao giờ toàn sàn!
                scope = VoucherScope.SHOP;
            } else if (scope == null) {
                scope = VoucherScope.PLATFORM;
            }

            VoucherTarget target = voucher.getVoucherTarget();
            if (target == null) {
                boolean isShipping = voucher.getVoucherType() == VoucherType.FREE_SHIPPING
                        || (voucher.getVoucherCode() != null && voucher.getVoucherCode().toUpperCase().contains("FREESHIP"));
                target = isShipping ? VoucherTarget.SHIPPING : VoucherTarget.ORDER;
            }

            boolean isPlatformScope = isCreatorAdmin && (scope == VoucherScope.PLATFORM || scope == VoucherScope.SYSTEM || scope == VoucherScope.WELCOME);

            if (!isPlatformScope) {
                shopVouchers.add(voucher);
            } else if (target == VoucherTarget.SHIPPING) {
                platformShippingVouchers.add(voucher);
            } else {
                platformOrderVouchers.add(voucher);
            }
        }

        // Theo dõi số tiền đã giảm theo từng hạng mục cho từng ShopOrderPreview
        Map<ShopOrderPreview, BigDecimal> shopGoodsDiscountMap = new HashMap<>();
        Map<ShopOrderPreview, BigDecimal> platformGoodsDiscountMap = new HashMap<>();
        Map<ShopOrderPreview, BigDecimal> platformShippingDiscountMap = new HashMap<>();

        for (ShopOrderPreview sp : shopPreviews) {
            shopGoodsDiscountMap.put(sp, BigDecimal.ZERO);
            platformGoodsDiscountMap.put(sp, BigDecimal.ZERO);
            platformShippingDiscountMap.put(sp, BigDecimal.ZERO);
        }

        // ============================================================
        // BƯỚC 1: Áp dụng Voucher của từng Shop (Giảm trước vào tiền hàng của chính shop đó)
        // ============================================================
        for (Voucher voucher : shopVouchers) {
            User shopOwner = voucher.getShop();
            if (shopOwner == null) continue;

            ShopOrderPreview targetShopPreview = shopPreviews.stream()
                    .filter(sp -> Objects.equals(sp.getShopId(), shopOwner.getId()))
                    .findFirst()
                    .orElse(null);
            if (targetShopPreview == null) continue;

            // ĐIỀU KIỆN ĐƠN TỐI THIỂU: Với voucher Của Shop, so sánh với TIỀN HÀNG CỦA CHÍNH SHOP ĐÓ
            if (voucher.getMinOrderValue() != null && targetShopPreview.getGoodsAmount().compareTo(voucher.getMinOrderValue()) < 0) {
                continue;
            }

            VoucherTarget target = voucher.getVoucherTarget() != null ? voucher.getVoucherTarget() : VoucherTarget.ORDER;
            BigDecimal discount = calculateVoucherDiscount(voucher, target, targetShopPreview.getGoodsAmount(), targetShopPreview.getShippingFee());
            if (discount.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Giới hạn giảm giá không vượt quá tiền hàng còn lại của shop đó
            BigDecimal currentGoodsDiscount = shopGoodsDiscountMap.get(targetShopPreview);
            BigDecimal remainingGoods = targetShopPreview.getGoodsAmount().subtract(currentGoodsDiscount).max(BigDecimal.ZERO);
            discount = discount.min(remainingGoods).setScale(0, java.math.RoundingMode.HALF_UP);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) continue;

            shopGoodsDiscountMap.put(targetShopPreview, currentGoodsDiscount.add(discount));
            targetShopPreview.setVoucherDiscount(targetShopPreview.getVoucherDiscount().add(discount));
            targetShopPreview.getVoucherDiscountsDetail().merge(voucher.getVoucherCode().toUpperCase(), discount, BigDecimal::add);
            totalDiscount = totalDiscount.add(discount);
        }

        // ============================================================
        // BƯỚC 2: Áp dụng Voucher Đơn Hàng Sàn (Phân bổ theo TỶ LỆ TIỀN HÀNG CÒN LẠI sau voucher shop)
        // ============================================================
        for (Voucher voucher : platformOrderVouchers) {
            if (voucher.getMinOrderValue() != null && totalGoodsAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                continue;
            }

            BigDecimal discount = calculateVoucherDiscount(voucher, VoucherTarget.ORDER, totalGoodsAmount, totalShippingFee);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Tính tiền hàng còn lại cho từng shop sau voucher shop và các voucher sàn trước đó
            Map<ShopOrderPreview, BigDecimal> availableNetGoodsMap = new HashMap<>();
            BigDecimal totalNetGoods = BigDecimal.ZERO;
            for (ShopOrderPreview sp : shopPreviews) {
                BigDecimal net = sp.getGoodsAmount()
                        .subtract(shopGoodsDiscountMap.get(sp))
                        .subtract(platformGoodsDiscountMap.get(sp))
                        .max(BigDecimal.ZERO);
                availableNetGoodsMap.put(sp, net);
                totalNetGoods = totalNetGoods.add(net);
            }

            if (totalNetGoods.compareTo(BigDecimal.ZERO) <= 0) continue;
            discount = discount.min(totalNetGoods).setScale(0, java.math.RoundingMode.HALF_UP);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Phân bổ tỷ lệ theo netGoods
            BigDecimal allocatedSum = BigDecimal.ZERO;
            Map<ShopOrderPreview, BigDecimal> shares = new HashMap<>();
            ShopOrderPreview maxShop = null;
            BigDecimal maxNet = BigDecimal.valueOf(-1);

            for (ShopOrderPreview sp : shopPreviews) {
                BigDecimal net = availableNetGoodsMap.get(sp);
                if (net.compareTo(maxNet) > 0) {
                    maxNet = net;
                    maxShop = sp;
                }
                if (net.compareTo(BigDecimal.ZERO) <= 0) {
                    shares.put(sp, BigDecimal.ZERO);
                    continue;
                }
                BigDecimal ratio = net.divide(totalNetGoods, 6, java.math.RoundingMode.HALF_UP);
                BigDecimal share = discount.multiply(ratio).setScale(0, java.math.RoundingMode.HALF_UP).min(net);
                shares.put(sp, share);
                allocatedSum = allocatedSum.add(share);
            }

            // Xử lý chênh lệch làm tròn VNĐ (cộng/trừ vào shop có netGoods lớn nhất)
            BigDecimal diff = discount.subtract(allocatedSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && maxShop != null) {
                BigDecimal currentShare = shares.get(maxShop);
                BigDecimal adjusted = currentShare.add(diff).min(availableNetGoodsMap.get(maxShop)).max(BigDecimal.ZERO);
                shares.put(maxShop, adjusted);
            }

            for (ShopOrderPreview sp : shopPreviews) {
                BigDecimal share = shares.getOrDefault(sp, BigDecimal.ZERO);
                if (share.compareTo(BigDecimal.ZERO) > 0) {
                    platformGoodsDiscountMap.put(sp, platformGoodsDiscountMap.get(sp).add(share));
                    sp.setVoucherDiscount(sp.getVoucherDiscount().add(share));
                    sp.getVoucherDiscountsDetail().merge(voucher.getVoucherCode().toUpperCase(), share, BigDecimal::add);
                }
            }
            totalDiscount = totalDiscount.add(discount);
        }

        // ============================================================
        // BƯỚC 3: Áp dụng Voucher Vận Chuyển Sàn (Phân bổ theo TỶ LỆ PHÍ SHIP của từng shop)
        // ============================================================
        for (Voucher voucher : platformShippingVouchers) {
            if (voucher.getMinOrderValue() != null && totalGoodsAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                continue;
            }

            BigDecimal discount = calculateVoucherDiscount(voucher, VoucherTarget.SHIPPING, totalGoodsAmount, totalShippingFee);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) continue;

            Map<ShopOrderPreview, BigDecimal> availableShippingMap = new HashMap<>();
            BigDecimal totalNetShipping = BigDecimal.ZERO;
            for (ShopOrderPreview sp : shopPreviews) {
                BigDecimal netShip = sp.getShippingFee().subtract(platformShippingDiscountMap.get(sp)).max(BigDecimal.ZERO);
                availableShippingMap.put(sp, netShip);
                totalNetShipping = totalNetShipping.add(netShip);
            }

            if (totalNetShipping.compareTo(BigDecimal.ZERO) <= 0) continue;
            discount = discount.min(totalNetShipping).setScale(0, java.math.RoundingMode.HALF_UP);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal allocatedSum = BigDecimal.ZERO;
            Map<ShopOrderPreview, BigDecimal> shares = new HashMap<>();
            ShopOrderPreview maxShop = null;
            BigDecimal maxShip = BigDecimal.valueOf(-1);

            for (ShopOrderPreview sp : shopPreviews) {
                BigDecimal netShip = availableShippingMap.get(sp);
                if (netShip.compareTo(maxShip) > 0) {
                    maxShip = netShip;
                    maxShop = sp;
                }
                if (netShip.compareTo(BigDecimal.ZERO) <= 0) {
                    shares.put(sp, BigDecimal.ZERO);
                    continue;
                }
                BigDecimal ratio = netShip.divide(totalNetShipping, 6, java.math.RoundingMode.HALF_UP);
                BigDecimal share = discount.multiply(ratio).setScale(0, java.math.RoundingMode.HALF_UP).min(netShip);
                shares.put(sp, share);
                allocatedSum = allocatedSum.add(share);
            }

            // Xử lý chênh lệch làm tròn VNĐ
            BigDecimal diff = discount.subtract(allocatedSum);
            if (diff.compareTo(BigDecimal.ZERO) != 0 && maxShop != null) {
                BigDecimal currentShare = shares.get(maxShop);
                BigDecimal adjusted = currentShare.add(diff).min(availableShippingMap.get(maxShop)).max(BigDecimal.ZERO);
                shares.put(maxShop, adjusted);
            }

            for (ShopOrderPreview sp : shopPreviews) {
                BigDecimal share = shares.getOrDefault(sp, BigDecimal.ZERO);
                if (share.compareTo(BigDecimal.ZERO) > 0) {
                    platformShippingDiscountMap.put(sp, platformShippingDiscountMap.get(sp).add(share));
                    sp.setVoucherDiscount(sp.getVoucherDiscount().add(share));
                    sp.getVoucherDiscountsDetail().merge(voucher.getVoucherCode().toUpperCase(), share, BigDecimal::add);
                }
            }
            totalDiscount = totalDiscount.add(discount);
        }

        return totalDiscount;
    }

    private BigDecimal calculateVoucherDiscount(Voucher voucher, VoucherTarget target, BigDecimal goodsAmount, BigDecimal shippingFee) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountValue = voucher.getDiscountValue() == null ? BigDecimal.ZERO : voucher.getDiscountValue();
        boolean isShipping = target == VoucherTarget.SHIPPING || voucher.getVoucherType() == VoucherType.FREE_SHIPPING;

        if (isShipping) {
            BigDecimal maxShippingFee = shippingFee == null ? BigDecimal.ZERO : shippingFee;
            if (voucher.getVoucherType() == VoucherType.PERCENTAGE) {
                BigDecimal percent = discountValue.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
                BigDecimal disc = maxShippingFee.multiply(percent);
                if (voucher.getMaxDiscount() != null && disc.compareTo(voucher.getMaxDiscount()) > 0) {
                    disc = voucher.getMaxDiscount();
                }
                return disc.min(maxShippingFee).max(BigDecimal.ZERO);
            } else {
                return discountValue.min(maxShippingFee).max(BigDecimal.ZERO);
            }
        }

        BigDecimal maxGoods = goodsAmount == null ? BigDecimal.ZERO : goodsAmount;
        if (voucher.getVoucherType() == VoucherType.PERCENTAGE) {
            BigDecimal percent = discountValue.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);
            BigDecimal disc = maxGoods.multiply(percent);
            if (voucher.getMaxDiscount() != null && disc.compareTo(voucher.getMaxDiscount()) > 0) {
                disc = voucher.getMaxDiscount();
            }
            return disc.min(maxGoods).max(BigDecimal.ZERO);
        } else {
            return discountValue.min(maxGoods).max(BigDecimal.ZERO);
        }
    }

    private boolean voucherAppliesToOrderShop(Voucher voucher, Order order) {
        if (voucher == null || order == null || order.getShop() == null) {
            return false;
        }

        User creator = voucher.getShop();
        String creatorRole = (creator != null && creator.getRole() != null) ? creator.getRole().getName() : "SHOP";
        boolean isCreatorAdmin = "ADMIN".equalsIgnoreCase(creatorRole);

        VoucherScope scope = voucher.getVoucherScope();
        if (!isCreatorAdmin) {
            scope = VoucherScope.SHOP;
        } else if (scope == null) {
            scope = VoucherScope.PLATFORM;
        }

        if (isCreatorAdmin && (scope == VoucherScope.PLATFORM || scope == VoucherScope.SYSTEM || scope == VoucherScope.WELCOME)) {
            return true;
        }

        return voucher.getShop() != null && Objects.equals(voucher.getShop().getId(), order.getShop().getId());
    }

    // ============================================================
    //  STATE MACHINE
    // ============================================================

    @Transactional
    @Override
    public OrderResponse markOrderAsConfirmed(int orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        if (order.getShop() != null && !Objects.equals(order.getShop().getId(), currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // Flow: PENDING / PAID -> CONFIRMED
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            log.error("Invalid state transition to CONFIRMED for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        try {
            if (saved.getBuyer() != null) {
                String shopName = saved.getShop() != null && saved.getShop().getFullName() != null
                        ? saved.getShop().getFullName() : "Nhà vườn";
                notificationService.sendNotificationToUser(
                        saved.getBuyer(),
                        "Nhà vườn đã xác nhận đơn hàng",
                        shopName + " đã xác nhận đơn hàng #" + saved.getOrderId() + " và đang chuẩn bị hàng.",
                        "ORDER_STATUS",
                        saved.getOrderId()
                );
            }
        } catch (Exception e) {
            log.warn("[Order] Failed to send order confirmed notification: {}", e.getMessage());
        }
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsReady(int orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        if (order.getShop() != null && !Objects.equals(order.getShop().getId(), currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // Flow: CONFIRMED -> READY
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.error("Invalid state transition to READY for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.READY);
        order.setReadyAt(LocalDateTime.now());
        order.setDispatchWave(null);
        order.setLastDispatchedAt(null);
        order.setDispatchAlertedAt(null);
        Order saved = orderRepository.save(order);
        // Bước 1: quét shipper trực tuyến quanh nhà vườn và mời nhận đơn (sau khi commit)
        dispatchService.dispatchAfterCommit(saved.getOrderId());
        dispatchService.notifyStatusChangedAfterCommit(saved.getOrderId());
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsShipping(int orderId) {
        // Khoá dòng đơn: 2 shipper bấm nhận cùng lúc thì người sau thấy đơn đã có chủ
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        User currentUser = getCurrentUser();
        // Flow: READY -> SHIPPING
        if (order.getShipper() != null && order.getShipper().getId() != currentUser.getId()) {
            throw new AppException(ErrorCode.ORDER_ALREADY_TAKEN);
        }
        if (order.getStatus() != OrderStatus.READY) {
            log.error("Invalid state transition to SHIPPING for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        if (!shipperService.isOnline(currentUser.getId())) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ONLINE);
        }

        // Bắt buộc 1 đơn tại 1 thời điểm: Kiểm tra xem shipper đã có đơn đang giao hoặc đang lấy hàng chưa (cả đơn xuôi và đơn hoàn)
        boolean hasActiveShipping = orderRepository.existsByShipper_IdAndStatusIn(
                currentUser.getId(), 
                List.of(OrderStatus.SHIPPING, OrderStatus.PICKED_UP)
        );
        boolean hasActiveReturn = returnRequestRepository.existsByShipper_IdAndStatusIn(
                currentUser.getId(),
                List.of(ReturnStatus.RETURN_SHIPPING, ReturnStatus.RETURN_PICKED_UP)
        );
        if (hasActiveShipping || hasActiveReturn) {
            log.warn("Shipper id {} already has an active order (forward: {}, return: {})", currentUser.getId(), hasActiveShipping, hasActiveReturn);
            throw new AppException(ErrorCode.SHIPPER_HAS_ACTIVE_ORDER);
        }

        order.setShipper(currentUser);
        order.setStatus(OrderStatus.SHIPPING);
        Order saved = orderRepository.saveAndFlush(order);
        // Ví âm (nợ COD cũ) bị chặn; đơn COD phải ký quỹ đủ tiền trong ví. Lỗi thì rollback cả việc nhận đơn
        walletService.reserveForShipperAccept(saved.getOrderId());
        // Shipper thấy ngay cước giao ở mục "tạm giữ", nhận thật khi đơn COMPLETED
        walletService.holdShipperIncome(saved.getOrderId());
        dispatchService.notifyOrderTakenAfterCommit(saved.getOrderId(), currentUser.getId());
        dispatchService.notifyStatusChangedAfterCommit(saved.getOrderId());
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsPickedUp(int orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        if (order.getShipper() != null && !Objects.equals(order.getShipper().getId(), currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // Flow: SHIPPING -> PICKED_UP
        if (order.getStatus() != OrderStatus.SHIPPING) {
            log.error("Invalid state transition to PICKED_UP for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.PICKED_UP);
        order.setPickedUpAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        dispatchService.notifyStatusChangedAfterCommit(saved.getOrderId());
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsDelivered(int orderId, DeliverOrderRequest request) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        if (order.getShipper() != null && !Objects.equals(order.getShipper().getId(), currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // Flow: PICKED_UP / SHIPPING -> DELIVERED
        if (order.getStatus() != OrderStatus.PICKED_UP && order.getStatus() != OrderStatus.SHIPPING) {
            log.error("Invalid state transition to DELIVERED for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        List<String> proofs = cleanUrls(request != null ? request.getProofImageUrls() : null);
        if (proofs.isEmpty()) {
            throw new AppException(ErrorCode.DELIVERY_PROOF_REQUIRED);
        }
        order.setDeliveryProofUrls(String.join("\n", proofs));
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        // === TÍCH LŨY SỐ KG NÔNG SẢN ĐÃ GIẢI CỨU (IDEMPOTENCY GUARD) ===
        if (order.getRescuedWeight() == null) {
            BigDecimal orderRescuedWeight = calculateOrderRescuedWeight(order);
            order.setRescuedWeight(orderRescuedWeight);

            User buyer = order.getBuyer();
            if (buyer != null) {
                BigDecimal currentRescued = buyer.getTotalRescuedWeight() != null
                        ? buyer.getTotalRescuedWeight()
                        : BigDecimal.ZERO;
                BigDecimal updatedRescued = currentRescued.add(orderRescuedWeight).setScale(2, java.math.RoundingMode.HALF_UP);
                buyer.setTotalRescuedWeight(updatedRescued);
                userRepository.save(buyer);
                log.info("[RescuedWeight] Order #{} DELIVERED: credited {} kg to buyer id {}. New total: {} kg",
                        orderId, orderRescuedWeight, buyer.getId(), updatedRescued);
            }
        }

        Order saved = orderRepository.save(order);
        shipperService.detachOrderFromShipperLocation(saved.getOrderId());

        // Đơn COD: Shipper vừa thu tiền mặt của khách -> ghi nợ vào ví Shipper cho tới khi nộp về sàn
        if ("COD".equalsIgnoreCase(saved.getPaymentMethod())) {
            walletService.recordCodCashCollected(saved.getOrderId());
            // Tiền mặt đã vào hệ thống -> Shop và Sàn thấy phần của mình ở mục "tạm giữ"
            walletService.holdShopAndPlatformIncome(saved.getOrderId());
        }

        // Cước giao hàng của Shipper được giải phóng và cộng thẳng vào số dư khả dụng ngay khi giao thành công
        walletService.releaseShipperFeeOnDelivered(saved.getOrderId());
        dispatchService.notifyStatusChangedAfterCommit(saved.getOrderId());
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    /** URL ảnh hợp lệ (http/https), bỏ trùng, tối đa 5 ảnh */
    private static List<String> cleanUrls(List<String> urls) {
        if (urls == null) return List.of();
        return urls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(u -> u.startsWith("http://") || u.startsWith("https://"))
                .distinct()
                .limit(5)
                .toList();
    }

    private static List<String> splitUrls(String joined) {
        if (joined == null || joined.isBlank()) return List.of();
        return java.util.Arrays.stream(joined.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private void restoreOrderVouchers(Order order) {
        List<VoucherUsage> usages = voucherUsageRepository.findByOrder(order);
        if (usages != null) {
            for (VoucherUsage usage : usages) {
                Voucher v = usage.getVoucher();
                boolean otherOrderStillUsing = false;
                if (order.getCheckoutBatch() != null && v != null) {
                    List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(order.getCheckoutBatch().getId());
                    if (batchOrders != null) {
                        for (Order bo : batchOrders) {
                            if (bo.getOrderId() != order.getOrderId() && bo.getStatus() != OrderStatus.CANCELLED && bo.getStatus() != OrderStatus.FAILED) {
                                List<VoucherUsage> boUsages = voucherUsageRepository.findByOrder(bo);
                                if (boUsages != null && boUsages.stream().anyMatch(u -> u.getVoucher() != null && u.getVoucher().getVoucherId() == v.getVoucherId())) {
                                    otherOrderStillUsing = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                // Xoá bản ghi VoucherUsage gắn với đơn này
                voucherUsageRepository.delete(usage);

                // CHỈ hoàn lại lượt dùng cho Buyer và Voucher nếu không còn đơn hàng nào khác trong batch đang hưởng voucher này
                if (!otherOrderStillUsing) {
                    if (v != null) {
                        v.setUsedCount(Math.max(0, (v.getUsedCount() != null ? v.getUsedCount() : 0) - 1));
                        voucherRepository.save(v);
                    }
                    if (order.getBuyer() != null && v != null) {
                        voucherUserRepository.findByUser_IdAndVoucher_VoucherId(order.getBuyer().getId(), v.getVoucherId())
                                .ifPresent(vu -> {
                                    vu.setQuantityUsed(Math.max(0, (vu.getQuantityUsed() != null ? vu.getQuantityUsed() : 0) - 1));
                                    voucherUserRepository.save(vu);
                                });
                    }
                }
            }
        }
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsCancelled(int orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        boolean isBuyer = order.getBuyer() != null && Objects.equals(order.getBuyer().getId(), currentUser.getId());
        boolean isShop = order.getShop() != null && Objects.equals(order.getShop().getId(), currentUser.getId());
        if (!isBuyer && !isShop) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // Chỉ được huỷ khi đơn đang ở trạng thái có thể huỷ (PENDING, PAID, CONFIRMED, READY)
        // CANCELLED / FAILED / DELIVERED / COMPLETED đều không được huỷ lại (tránh hoàn kho 2 lần)
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT
                && order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.READY) {
            log.error("Cannot cancel order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }

        // Trả lại kho (chỉ chạy 1 lần vì status đã được guard ở trên)
        restoreStock(order);
        restoreOrderVouchers(order);


        // Nếu đơn hàng bị huỷ khi chưa hoàn tất thanh toán (AWAITING_PAYMENT), hoàn trả lại giỏ hàng cho Buyer
        // và huỷ link PayOS đang mở để khách không thể chuyển khoản cho đơn đã huỷ
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            cartService.restoreOrderItemsToCart(order);
            paymentService.cancelOpenPayosLinksForOrder(order);
        }

        // Đơn đã thanh toán online mà bị huỷ -> ghi nhận số tiền sàn phải hoàn cho Buyer
        if (isPrepaidOnline(order)) {
            order.setRefundAmount(order.getTotalAmount());
            order.setRefundReason(isBuyer ? "Buyer huỷ đơn đã thanh toán online" : "Shop huỷ đơn đã thanh toán online");
        }

        walletService.cancelHolds(order.getOrderId(), "đơn bị huỷ");
        referralService.handleOrderCancelled(order.getOrderId());
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        try {
            String canceller = isBuyer ? "Người mua" : "Nhà vườn";
            if (isBuyer && saved.getShop() != null) {
                notificationService.sendNotificationToUser(
                        saved.getShop(),
                        "Đơn hàng đã bị hủy",
                        "Người mua đã hủy đơn hàng #" + saved.getOrderId() + ".",
                        "ORDER_STATUS",
                        saved.getOrderId()
                );
            } else if (!isBuyer && saved.getBuyer() != null) {
                notificationService.sendNotificationToUser(
                        saved.getBuyer(),
                        "Đơn hàng đã bị hủy",
                        "Nhà vườn đã hủy đơn hàng #" + saved.getOrderId() + ".",
                        "ORDER_STATUS",
                        saved.getOrderId()
                );
            }
            if (saved.getShipper() != null) {
                notificationService.sendNotificationToUser(
                        saved.getShipper(),
                        "Đơn hàng đã bị hủy",
                        "Đơn hàng #" + saved.getOrderId() + " đã bị hủy bởi " + canceller + ".",
                        "ORDER_STATUS",
                        saved.getOrderId()
                );
            }
        } catch (Exception e) {
            log.warn("[Order] Failed to send order cancelled notification: {}", e.getMessage());
        }
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsFailed(int orderId, FailOrderRequest request) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        if (order.getShipper() != null && !Objects.equals(order.getShipper().getId(), currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        // Đơn đang SHIPPING hoặc PICKED_UP mới có thể báo giao thất bại
        if (order.getStatus() != OrderStatus.SHIPPING && order.getStatus() != OrderStatus.PICKED_UP) {
            log.error("Invalid state transition to FAILED for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        
        // Bằng chứng: lý do, số cuộc gọi (>= 3 nếu không liên lạc được), ảnh
        if (request == null || request.getReason() == null) {
            throw new AppException(ErrorCode.FAIL_REASON_REQUIRED);
        }
        String note = request.getNote() != null ? request.getNote().trim() : "";
        int calls = request.getCallAttempts() != null ? Math.max(0, request.getCallAttempts()) : 0;
        if (request.getReason() == com.sang.be_capnong.enums.DeliveryFailReason.KHONG_LIEN_LAC_DUOC && calls < 3) {
            throw new AppException(ErrorCode.FAIL_CALL_ATTEMPTS_REQUIRED);
        }
        if (request.getReason() == com.sang.be_capnong.enums.DeliveryFailReason.KHAC && note.isEmpty()) {
            throw new AppException(ErrorCode.FAIL_NOTE_REQUIRED);
        }
        List<String> proofs = cleanUrls(request.getProofImageUrls());
        if (proofs.isEmpty()) {
            throw new AppException(ErrorCode.DELIVERY_PROOF_REQUIRED);
        }
        order.setFailReason(request.getReason());
        order.setFailNote(note.isEmpty() ? null : note.substring(0, Math.min(note.length(), 500)));
        order.setFailCallAttempts(calls);
        order.setFailProofUrls(String.join("\n", proofs));
        order.setFailedAt(LocalDateTime.now());

        // Giao hàng thất bại -> Hàng trả về shop -> Trả lại kho
        restoreStock(order);
        restoreOrderVouchers(order);

        // Đơn đã thanh toán online mà giao thất bại -> ghi nhận số tiền sàn phải hoàn cho Buyer
        if (isPrepaidOnline(order)) {
            order.setRefundAmount(order.getTotalAmount());
            order.setRefundReason("Giao hàng thất bại, đơn đã thanh toán online");
        }

        walletService.cancelHolds(order.getOrderId(), "giao hàng thất bại");
        // Đơn COD: trả lại tiền ký quỹ cho Shipper
        walletService.releaseCodReserve(order.getOrderId(), "giao hàng thất bại");
        referralService.handleOrderCancelled(order.getOrderId());
        order.setStatus(OrderStatus.FAILED);
        Order saved = orderRepository.save(order);
        shipperService.detachOrderFromShipperLocation(saved.getOrderId());
        dispatchService.notifyStatusChangedAfterCommit(saved.getOrderId());
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsCompleted(int orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        if (order.getBuyer() == null || !Objects.equals(order.getBuyer().getId(), currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return completeAndSettle(order);
    }

    @Transactional
    @Override
    public OrderResponse autoCompleteOrder(int orderId) {
        return completeAndSettle(findOrderById(orderId));
    }

    /** DELIVERED -> COMPLETED rồi chia tiền vào ví trong cùng 1 transaction (lỗi thì rollback cả hai) */
    private OrderResponse completeAndSettle(Order order) {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            log.error("Invalid state transition to COMPLETED for order id {}. Current status: {}", order.getOrderId(), order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        Order saved = orderRepository.saveAndFlush(order);
        walletService.settleOrder(saved.getOrderId());
        try {
            if (saved.getShop() != null) {
                notificationService.sendNotificationToUser(
                        saved.getShop(),
                        "Đơn hàng hoàn tất",
                        "Đơn hàng #" + saved.getOrderId() + " đã hoàn tất. Tiền bán hàng đã được cộng vào ví của bạn.",
                        "ORDER_STATUS",
                        saved.getOrderId()
                );
            }
            if (saved.getBuyer() != null) {
                notificationService.sendNotificationToUser(
                        saved.getBuyer(),
                        "Đơn hàng hoàn tất",
                        "Đơn hàng #" + saved.getOrderId() + " đã hoàn tất. Cảm ơn bạn đã mua sắm nông sản!",
                        "ORDER_STATUS",
                        saved.getOrderId()
                );
            }
        } catch (Exception e) {
            log.warn("[Order] Failed to send order completed notification: {}", e.getMessage());
        }
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    // ============================================================
    //  PRIVATE HELPERS
    // ============================================================

    /** Đơn thanh toán online (PayOS / chuyển khoản) và tiền đã vào tài khoản sàn */
    private boolean isPrepaidOnline(Order order) {
        boolean online = "PAYOS".equalsIgnoreCase(order.getPaymentMethod()) || "TRANSFER".equalsIgnoreCase(order.getPaymentMethod());
        if (!online) return false;
        OrderStatus st = order.getStatus();
        return st == OrderStatus.PAID || st == OrderStatus.CONFIRMED || st == OrderStatus.READY
                || st == OrderStatus.SHIPPING || st == OrderStatus.PICKED_UP;
    }

    private void restoreStock(Order order) {
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
        for (OrderDetail detail : details) {
            int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
            if (qty <= 0) continue;

            if ("PRODUCT".equals(detail.getItemType()) && detail.getProduct() != null) {
                productRepository.increaseStockAtomic(detail.getProduct().getProductId(), qty);
            } else if ("MYSTERY_BOX".equals(detail.getItemType()) && detail.getMysteryBox() != null) {
                mysteryBoxRepository.increaseStockAtomic(detail.getMysteryBox().getMysteryId(), qty);
            } else if ("BUILD_COMBO".equals(detail.getItemType()) && detail.getBuildCombo() != null) {
                buildComboRepository.increaseStockAtomic(detail.getBuildCombo().getComboId(), qty);
            }
        }
    }

    /**
     * Lấy User hiện tại đang đăng nhập từ Spring Security context.
     *
     * JWT subject = user.getId() (số nguyên) — xem AuthenticationServiceImpl.generateToken()
     * → dùng findById, KHÔNG dùng findByEmail
     */
    private User getCurrentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        int userId = Integer.parseInt(subject);
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Order findOrderById(int orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * Quy tắc: mỗi OrderItem chỉ được có đúng 1 loại ID
     */
    private void validateItemHasExactlyOneId(OrderItemRequest item) {
        int count = 0;
        if (item.getProductId() != null) count++;
        if (item.getMysteryBoxId() != null) count++;
        if (item.getBuildComboId() != null) count++;
        if (count != 1) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
    }

    /**
     * COD / TRANSFER / PAYOS: Đơn hàng mới tạo đều khởi đầu ở trạng thái PENDING.
     * Đối với thanh toán online (TRANSFER/PAYOS), đơn sẽ chuyển sang PAID sau khi hoàn tất thanh toán.
     * Cả PENDING (đơn COD) và PAID (đơn chuyển khoản) sẽ được Farmer/Shop duyệt.
     */
    private OrderStatus resolveInitialStatus(String paymentMethod) {
        if ("PAYOS".equalsIgnoreCase(paymentMethod) || "TRANSFER".equalsIgnoreCase(paymentMethod)) {
            return OrderStatus.AWAITING_PAYMENT;
        }
        return OrderStatus.PENDING;
    }

    @Override
    public OrderResponse getOrderById(int orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();

        boolean isBuyer = order.getBuyer() != null && Objects.equals(order.getBuyer().getId(), currentUser.getId());
        boolean isShop = order.getShop() != null && Objects.equals(order.getShop().getId(), currentUser.getId());
        boolean isShipper = order.getShipper() != null && Objects.equals(order.getShipper().getId(), currentUser.getId());
        boolean isAdminOrStaff = currentUser.getRole() != null && (
                "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()) ||
                "STAFF".equalsIgnoreCase(currentUser.getRole().getName())
        );

        if (!isBuyer && !isShop && !isShipper && !isAdminOrStaff) {
            log.warn("Access Denied: User id {} has no permission to view order id {}", currentUser.getId(), orderId);
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(order, details);
    }

    @Override
    public Page<OrderResponse> getMyOrders(int page, int size, List<OrderStatus> statuses) {
        User buyer = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders;
        if (statuses != null && !statuses.isEmpty()) {
            orders = orderRepository.findByBuyer_IdAndStatusIn(buyer.getId(), statuses, pageable);
        } else {
            orders = orderRepository.findByBuyer_Id(buyer.getId(), pageable);
        }
        return orders.map(order -> {
            List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : Collections.emptyList();
            return buildOrderResponse(order, details);
        });
    }

    @Override
    public Page<OrderResponse> getShopOrders(int page, int size, List<OrderStatus> statuses) {
        User shop = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders;
        if (statuses != null && !statuses.isEmpty()) {
            // Loại bỏ hoàn toàn AWAITING_PAYMENT vì trạng thái này chỉ dành cho buyer thanh toán, shop chưa duyệt
            List<OrderStatus> validStatuses = statuses.stream()
                    .filter(s -> s != OrderStatus.AWAITING_PAYMENT)
                    .toList();
            if (validStatuses.isEmpty()) {
                return org.springframework.data.domain.Page.empty(pageable);
            }
            orders = orderRepository.findByShop_IdAndStatusIn(shop.getId(), validStatuses, pageable);
        } else {
            // Khi không truyền statuses (hoặc xem tất cả), loại trừ AWAITING_PAYMENT khỏi phía Shop
            orders = orderRepository.findByShop_IdAndStatusNotIn(
                    shop.getId(),
                    java.util.List.of(OrderStatus.AWAITING_PAYMENT),
                    pageable
            );
        }
        return orders.map(order -> {
            List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : Collections.emptyList();
            return buildOrderResponse(order, details);
        });
    }

    @Override
    public Page<OrderResponse> getReadyOrders(int page, int size, Double latitude, Double longitude) {
        User shipper = getCurrentUser();
        if (!shipperService.isOnline(shipper.getId())) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ONLINE);
        }
        double[] me = latitude != null && longitude != null
                ? new double[]{latitude, longitude}
                : shipperService.getLastPosition(shipper.getId());

        // Số đơn READY tại 1 thời điểm nhỏ: lọc + sắp theo khoảng cách trong bộ nhớ rồi tự phân trang
        List<OrderResponse> nearby = new ArrayList<>();
        for (Order order : orderRepository.findByStatusAndShipperIsNull(OrderStatus.READY)) {
            List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : Collections.emptyList();
            OrderResponse res = buildOrderResponse(order, details);
            if (me != null && res.getShopLatitude() != null && res.getShopLongitude() != null) {
                double km = com.sang.be_capnong.util.GeoUtils.roadKm(me[0], me[1], res.getShopLatitude(), res.getShopLongitude());
                if (km > READY_LIST_MAX_KM) continue;
                res.setDistanceToShopKm(km);
            }
            nearby.add(res);
        }
        nearby.sort(Comparator.comparing((OrderResponse r) -> r.getDistanceToShopKm() == null ? Double.MAX_VALUE : r.getDistanceToShopKm())
                .thenComparing(OrderResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        int safeSize = Math.max(1, Math.min(size, 100));
        int from = Math.min(Math.max(page, 0) * safeSize, nearby.size());
        int to = Math.min(from + safeSize, nearby.size());
        return new org.springframework.data.domain.PageImpl<>(nearby.subList(from, to), PageRequest.of(Math.max(page, 0), safeSize), nearby.size());
    }

    @Override
    public Page<OrderResponse> getShipperOrders(int page, int size, List<OrderStatus> statuses) {
        User shipper = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders;
        if (statuses != null && !statuses.isEmpty()) {
            orders = orderRepository.findByShipper_IdAndStatusIn(shipper.getId(), statuses, pageable);
        } else {
            orders = orderRepository.findByShipper_Id(shipper.getId(), pageable);
        }
        return orders.map(order -> {
            List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : Collections.emptyList();
            return buildOrderResponse(order, details);
        });
    }

    /**
     * Lấy giá bán thực tế của sản phẩm (tính discount nếu đang còn hiệu lực)
     */
    private BigDecimal resolveProductPrice(Product product) {
        BigDecimal basePrice = product.getSellingPrice();
        if (basePrice == null) return BigDecimal.ZERO;

        Integer discountPercent = product.getDiscountPercent();
        LocalDateTime now = LocalDateTime.now();

        boolean hasDiscount = discountPercent != null && discountPercent > 0
                && product.getDiscountStartDate() != null
                && product.getDiscountEndDate() != null
                && now.isAfter(product.getDiscountStartDate())
                && now.isBefore(product.getDiscountEndDate());

        if (hasDiscount) {
            BigDecimal multiplier = BigDecimal.ONE.subtract(
                    BigDecimal.valueOf(discountPercent).divide(BigDecimal.valueOf(100))
            );
            return basePrice.multiply(multiplier);
        }
        return basePrice;
    }

    /**
     * Thủ công build OrderResponse khi createOrder hoặc query
     */
    private OrderResponse buildOrderResponse(Order order, List<OrderDetail> details) {
        List<OrderDetailResponse> itemResponses = new ArrayList<>();
        for (OrderDetail d : details) {
            String name = "";
            String imageUrl = "";

            if ("PRODUCT".equals(d.getItemType()) && d.getProduct() != null) {
                name = d.getProduct().getProductName();
                imageUrl = d.getProduct().getImages() != null && !d.getProduct().getImages().isEmpty()
                        ? d.getProduct().getImages().get(0).getImageUrl() : "";
            } else if ("MYSTERY_BOX".equals(d.getItemType()) && d.getMysteryBox() != null) {
                name = d.getMysteryBox().getBoxType();
                imageUrl = d.getMysteryBox().getImageUrl() != null ? d.getMysteryBox().getImageUrl() : "";
            } else if ("BUILD_COMBO".equals(d.getItemType()) && d.getBuildCombo() != null) {
                name = d.getBuildCombo().getComboName();
                imageUrl = d.getBuildCombo().getImageUrl() != null ? d.getBuildCombo().getImageUrl() : "";
            }

            BigDecimal subtotal = d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));

            itemResponses.add(OrderDetailResponse.builder()
                    .orderDetailId(d.getOrderDetailId())
                    .itemType(d.getItemType())
                    .itemName(name)
                    .imageUrl(imageUrl)
                    .quantity(d.getQuantity())
                    .unitPrice(d.getUnitPrice())
                    .subtotal(subtotal)
                    .isReviewed(d.getReviews() != null && !d.getReviews().isEmpty())
                    .isRequestedReturn(Boolean.TRUE.equals(d.getIsRequestedReturn()))
                    .build());
        }

        BigDecimal codAmount = BigDecimal.ZERO;
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            // cod_amount chỉ có giá trị thật sau khi giao (tiền mặt đã thu); trước đó (NULL hoặc 0 trong dữ liệu mẫu) = tổng tiền đơn
            codAmount = order.getCodAmount() != null && order.getCodAmount().signum() > 0 ? order.getCodAmount() : order.getTotalAmount();
        }

        Double shopLat = order.getShopLatitude();
        Double shopLng = order.getShopLongitude();
        if (shopLat == null && order.getShop() != null) {
            String addr = order.getShop().getAddress();
            if (addr != null && !addr.isBlank()) {
                try {
                    double[] coords = googleMapService.geocodeAddress(addr);
                    if (coords != null && coords.length == 2) {
                        shopLat = coords[0];
                        shopLng = coords[1];
                        order.setShopLatitude(shopLat);
                        order.setShopLongitude(shopLng);
                    }
                } catch (Exception ignored) {}
            }
        }
        if (shopLat == null) { shopLat = 10.8500; shopLng = 106.7720; }

        Double destLat = order.getShippingLatitude();
        Double destLng = order.getShippingLongitude();
        if (destLat == null && order.getShippingAddress() != null && !order.getShippingAddress().isBlank()) {
            try {
                double[] coords = googleMapService.geocodeAddress(order.getShippingAddress());
                if (coords != null && coords.length == 2) {
                    destLat = coords[0];
                    destLng = coords[1];
                    order.setShippingLatitude(destLat);
                    order.setShippingLongitude(destLng);
                }
            } catch (Exception ignored) {}
        }
        if (destLat == null) { destLat = 10.7725; destLng = 106.6578; }

        Double distanceKm = null;
        if (shopLat != null && shopLng != null && destLat != null && destLng != null) {
            double r = 6371;
            double dLat = Math.toRadians(destLat - shopLat);
            double dLon = Math.toRadians(destLng - shopLng);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(shopLat)) * Math.cos(Math.toRadians(destLat))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double straightKm = r * c;
            double roadDist = Math.max(0.4, straightKm * 1.25);
            distanceKm = Math.round(roadDist * 10.0) / 10.0;
        }

        BigDecimal voucherDiscountAmount = BigDecimal.ZERO;
        if (order.getVoucherUsages() != null && !order.getVoucherUsages().isEmpty()) {
            voucherDiscountAmount = order.getVoucherUsages().stream()
                    .map(VoucherUsage::getDiscountAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if (order.getOrderId() > 0) {
            List<VoucherUsage> usages = voucherUsageRepository.findByOrder_OrderId(order.getOrderId());
            if (usages != null && !usages.isEmpty()) {
                voucherDiscountAmount = usages.stream()
                        .map(VoucherUsage::getDiscountAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        }

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .batchId(order.getCheckoutBatch() != null ? order.getCheckoutBatch().getId() : null)
                .shopId(order.getShop() != null ? order.getShop().getId() : null)
                .buyerName(order.getBuyer() != null ? order.getBuyer().getFullName() : null)
                .shopName(order.getShop() != null ? (order.getShop().getShopName() != null ? order.getShop().getShopName() : order.getShop().getFullName()) : null)
                .shopAddress(order.getShop() != null ? order.getShop().getAddress() : null)
                .shopPhone(order.getShop() != null ? order.getShop().getPhoneNumber() : null)
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .codAmount(codAmount)
                .voucherDiscountAmount(voucherDiscountAmount)
                .shipperId(order.getShipper() != null ? order.getShipper().getId() : null)
                .shipperName(order.getShipper() != null ? order.getShipper().getFullName() : null)
                .createdAt(order.getCreatedAt())
                .pickedUpAt(order.getPickedUpAt())
                .shopLatitude(shopLat)
                .shopLongitude(shopLng)
                .shippingLatitude(destLat)
                .shippingLongitude(destLng)
                .distanceKm(distanceKm)
                .rescuedWeight(order.getRescuedWeight())
                .refundAmount(order.getRefundAmount())
                .codReservedAmount(order.getCodReservedAmount())
                .deliveredAt(order.getDeliveredAt())
                .deliveryProofUrls(splitUrls(order.getDeliveryProofUrls()))
                .failReason(order.getFailReason() != null ? order.getFailReason().name() : null)
                .failNote(order.getFailNote())
                .failCallAttempts(order.getFailCallAttempts())
                .failProofUrls(splitUrls(order.getFailProofUrls()))
                .failedAt(order.getFailedAt())
                .items(itemResponses)
                .build();
    }

    /**
     * Quy đổi đơn vị tính của nông sản sang kilôgam (kg)
     */
    private BigDecimal getKgPerUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return BigDecimal.valueOf(1.0);
        }
        String u = unit.trim().toLowerCase();
        if (u.contains("kg") || u.contains("kilogram") || u.contains("ký") || u.contains("kí")) {
            return BigDecimal.valueOf(1.0);
        } else if (u.contains("gram") || u.equals("g")) {
            return BigDecimal.valueOf(0.001);
        } else if (u.contains("bó") || u.contains("bo")) {
            return BigDecimal.valueOf(0.4);
        } else if (u.contains("túi") || u.contains("gói") || u.contains("khay") || u.contains("tui") || u.contains("goi")) {
            return BigDecimal.valueOf(0.5);
        } else if (u.contains("trái") || u.contains("quả") || u.contains("qua") || u.contains("trai")) {
            return BigDecimal.valueOf(1.2);
        } else if (u.contains("hộp") || u.contains("phần") || u.contains("hop") || u.contains("phan")) {
            return BigDecimal.valueOf(0.5);
        }
        return BigDecimal.valueOf(1.0);
    }

    /**
     * Tính tổng số kg nông sản được giải cứu trong đơn hàng
     */
    private BigDecimal calculateOrderRescuedWeight(Order order) {
        List<OrderDetail> details = (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty())
                ? order.getOrderDetails()
                : orderDetailRepository.findByOrder_OrderId(order.getOrderId());

        if (details == null || details.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalOrderWeight = BigDecimal.ZERO;

        for (OrderDetail d : details) {
            int quantity = d.getQuantity() != null && d.getQuantity() > 0 ? d.getQuantity() : 1;
            BigDecimal itemTotalWeight = BigDecimal.ZERO;

            if ("PRODUCT".equalsIgnoreCase(d.getItemType()) || d.getProduct() != null) {
                Product p = d.getProduct();
                if (p != null) {
                    BigDecimal kgPerUnit = getKgPerUnit(p.getUnit());
                    itemTotalWeight = kgPerUnit.multiply(BigDecimal.valueOf(quantity));
                } else {
                    itemTotalWeight = BigDecimal.valueOf(quantity);
                }
            } else if ("MYSTERY_BOX".equalsIgnoreCase(d.getItemType()) || d.getMysteryBox() != null) {
                MysteryBox box = d.getMysteryBox();
                BigDecimal singleBoxWeight = BigDecimal.ZERO;
                if (box != null) {
                    List<ProductMystery> items = productMysteryRepository.findByMysteryBox_MysteryId(box.getMysteryId());
                    if (items != null && !items.isEmpty()) {
                        for (ProductMystery pm : items) {
                            if (pm.getProduct() != null) {
                                int pQty = pm.getProductQuantity() != null && pm.getProductQuantity() > 0 ? pm.getProductQuantity() : 1;
                                BigDecimal pKg = getKgPerUnit(pm.getProduct().getUnit());
                                singleBoxWeight = singleBoxWeight.add(pKg.multiply(BigDecimal.valueOf(pQty)));
                            }
                        }
                    }
                }
                if (singleBoxWeight.compareTo(BigDecimal.ZERO) <= 0) {
                    singleBoxWeight = BigDecimal.valueOf(3.0);
                }
                itemTotalWeight = singleBoxWeight.multiply(BigDecimal.valueOf(quantity));
            } else if ("BUILD_COMBO".equalsIgnoreCase(d.getItemType()) || d.getBuildCombo() != null) {
                BuildCombo combo = d.getBuildCombo();
                BigDecimal singleComboWeight = BigDecimal.ZERO;
                if (combo != null) {
                    List<ProductCombo> items = productComboRepository.findByBuildComboComboId(combo.getComboId());
                    if (items != null && !items.isEmpty()) {
                        for (ProductCombo pc : items) {
                            if (pc.getProduct() != null) {
                                int pQty = pc.getProductQuantity() != null && pc.getProductQuantity() > 0 ? pc.getProductQuantity() : 1;
                                BigDecimal pKg = getKgPerUnit(pc.getProduct().getUnit());
                                singleComboWeight = singleComboWeight.add(pKg.multiply(BigDecimal.valueOf(pQty)));
                            }
                        }
                    }
                }
                if (singleComboWeight.compareTo(BigDecimal.ZERO) <= 0) {
                    singleComboWeight = BigDecimal.valueOf(2.0);
                }
                itemTotalWeight = singleComboWeight.multiply(BigDecimal.valueOf(quantity));
            } else {
                itemTotalWeight = BigDecimal.valueOf(quantity);
            }

            totalOrderWeight = totalOrderWeight.add(itemTotalWeight);
        }

        return totalOrderWeight.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
