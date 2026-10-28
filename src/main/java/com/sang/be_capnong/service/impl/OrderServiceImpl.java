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
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.OrderMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.GoogleMapService;
import com.sang.be_capnong.service.OrderService;
import com.sang.be_capnong.service.ShippingFeeService;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.enums.VoucherScope;
import com.sang.be_capnong.enums.VoucherTarget;
import com.sang.be_capnong.enums.VoucherType;
import com.sang.be_capnong.dto.request.ShippingFeeRequest;
import com.sang.be_capnong.dto.response.ShippingFeeResponse;
import java.util.Objects;
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
    UserRepository userRepository;
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
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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
            BigDecimal shopTotal = sp.getGoodsAmount().add(sp.getShippingFee()).subtract(sp.getVoucherDiscount());
            if (shopTotal.compareTo(BigDecimal.ZERO) < 0) shopTotal = BigDecimal.ZERO;
            sp.setTotalAmount(shopTotal);
        }

        BigDecimal finalTotalAmount = shopOrderPreviews.stream()
                .map(ShopOrderPreview::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal effectiveTotalDiscount = totalGoodsAmount.add(totalShippingFee).subtract(finalTotalAmount);
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
        if (voucherCodes == null || voucherCodes.isEmpty()) return totalDiscount;

        for (String code : voucherCodes) {
            Voucher voucher = voucherRepository.findByVoucherCode(code.toUpperCase()).orElse(null);
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

            if (isPlatformScope) {
                // ĐIỀU KIỆN ĐƠN TỐI THIỂU: Với voucher Toàn sàn (cả Freeship và Giảm giá), đơn tối thiểu luôn so sánh với TỔNG TIỀN HÀNG
                if (voucher.getMinOrderValue() != null && totalGoodsAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                    continue;
                }

                BigDecimal discount = calculateVoucherDiscount(voucher, target, totalGoodsAmount, totalShippingFee);
                if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                totalDiscount = totalDiscount.add(discount);
                BigDecimal baseTotal = target == VoucherTarget.SHIPPING ? totalShippingFee : totalGoodsAmount;
                if (shopPreviews.isEmpty() || baseTotal.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                for (ShopOrderPreview sp : shopPreviews) {
                    BigDecimal spBase = target == VoucherTarget.SHIPPING ? sp.getShippingFee() : sp.getGoodsAmount();
                    if (spBase.compareTo(BigDecimal.ZERO) <= 0) continue;

                    BigDecimal ratio = spBase.divide(baseTotal, 6, java.math.RoundingMode.HALF_UP);
                    BigDecimal shopShare = discount.multiply(ratio);
                    sp.setVoucherDiscount(sp.getVoucherDiscount().add(shopShare));
                    sp.getVoucherDiscountsDetail().put(code.toUpperCase(), shopShare);
                }
                continue;
            }

            User shopOwner = voucher.getShop();
            if (shopOwner == null) {
                continue;
            }

            ShopOrderPreview targetShopPreview = shopPreviews.stream()
                    .filter(sp -> Objects.equals(sp.getShopId(), shopOwner.getId()))
                    .findFirst()
                    .orElse(null);
            if (targetShopPreview == null) {
                // Voucher của shop này không áp dụng vì đơn hàng không có sản phẩm của shop này
                continue;
            }

            // ĐIỀU KIỆN ĐƠN TỐI THIỂU: Với voucher Của Shop, đơn tối thiểu so sánh với TIỀN HÀNG CỦA CHÍNH SHOP ĐÓ
            if (voucher.getMinOrderValue() != null && targetShopPreview.getGoodsAmount().compareTo(voucher.getMinOrderValue()) < 0) {
                continue;
            }

            BigDecimal discount = calculateVoucherDiscount(voucher, target, targetShopPreview.getGoodsAmount(), targetShopPreview.getShippingFee());
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Giới hạn giảm giá không vượt quá số tiền tối đa còn lại của shop đó (tránh tràn số âm sang shop khác)
            BigDecimal remainingShopPayable = targetShopPreview.getGoodsAmount().add(targetShopPreview.getShippingFee()).subtract(targetShopPreview.getVoucherDiscount());
            if (remainingShopPayable.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            discount = discount.min(remainingShopPayable);

            targetShopPreview.setVoucherDiscount(targetShopPreview.getVoucherDiscount().add(discount));
            targetShopPreview.getVoucherDiscountsDetail().put(code.toUpperCase(), discount);
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
        Order saved = orderRepository.save(order);
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsShipping(int orderId) {
        Order order = findOrderById(orderId);
        User currentUser = getCurrentUser();
        // Flow: READY -> SHIPPING
        if (order.getStatus() != OrderStatus.READY) {
            log.error("Invalid state transition to SHIPPING for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }

        // Bắt buộc 1 đơn tại 1 thời điểm: Kiểm tra xem shipper đã có đơn đang giao hoặc đang lấy hàng chưa
        boolean hasActiveShipping = orderRepository.existsByShipper_IdAndStatusIn(
                currentUser.getId(), 
                List.of(OrderStatus.SHIPPING, OrderStatus.PICKED_UP)
        );
        if (hasActiveShipping) {
            log.warn("Shipper id {} already has an active shipping order", currentUser.getId());
            throw new AppException(ErrorCode.SHIPPER_HAS_ACTIVE_ORDER);
        }

        order.setShipper(currentUser);
        order.setStatus(OrderStatus.SHIPPING);
        Order saved = orderRepository.save(order);
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
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsDelivered(int orderId) {
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
        order.setStatus(OrderStatus.DELIVERED);
        Order saved = orderRepository.save(order);
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
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

        // Hoàn trả Voucher nếu có
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

        // Nếu đơn hàng bị huỷ khi chưa hoàn tất thanh toán (AWAITING_PAYMENT), hoàn trả lại giỏ hàng cho Buyer
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            cartService.restoreOrderItemsToCart(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsFailed(int orderId) {
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
        
        // Giao hàng thất bại -> Hàng trả về shop -> Trả lại kho
        restoreStock(order);
        
        order.setStatus(OrderStatus.FAILED);
        Order saved = orderRepository.save(order);
        List<OrderDetail> details = saved.getOrderDetails() != null ? saved.getOrderDetails() : Collections.emptyList();
        return buildOrderResponse(saved, details);
    }

    // ============================================================
    //  PRIVATE HELPERS
    // ============================================================

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
    public Page<OrderResponse> getReadyOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByStatus(OrderStatus.READY, pageable);
        return orders.map(order -> {
            List<OrderDetail> details = order.getOrderDetails() != null ? order.getOrderDetails() : Collections.emptyList();
            return buildOrderResponse(order, details);
        });
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
                    .build());
        }

        BigDecimal codAmount = BigDecimal.ZERO;
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            codAmount = order.getCodAmount() != null ? order.getCodAmount() : order.getTotalAmount();
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
                .shipperId(order.getShipper() != null ? order.getShipper().getId() : null)
                .shipperName(order.getShipper() != null ? order.getShipper().getFullName() : null)
                .createdAt(order.getCreatedAt())
                .pickedUpAt(order.getPickedUpAt())
                .shopLatitude(shopLat)
                .shopLongitude(shopLng)
                .shippingLatitude(destLat)
                .shippingLongitude(destLng)
                .distanceKm(distanceKm)
                .items(itemResponses)
                .build();
    }
}
