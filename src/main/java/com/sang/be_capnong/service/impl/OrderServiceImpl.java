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
import com.sang.be_capnong.service.OrderService;
import com.sang.be_capnong.service.ShippingFeeService;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.enums.VoucherType;
import com.sang.be_capnong.dto.request.ShippingFeeRequest;
import com.sang.be_capnong.dto.response.ShippingFeeResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        // 3. Tạo Order dựa trên preview
        for (ShopOrderPreview sp : previewRes.getShopOrders()) {
            User shop = userRepository.findById(sp.getShopId()).orElseThrow();
            List<OrderDetail> details = mapShopToDetails.get(shop);

            Order order = Order.builder()
                    .buyer(buyer)
                    .shop(shop)
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
            responses.add(res);
        }

        // 4. Cập nhật lịch sử dùng Voucher (Lưu discountAmount vào VoucherUsage)
        if (request.getVoucherCodes() != null && !request.getVoucherCodes().isEmpty()) {
            for (String code : request.getVoucherCodes()) {
                Voucher v = voucherRepository.findByVoucherCode(code.toUpperCase()).orElse(null);
                if (v != null) {
                    v.setUsedCount((v.getUsedCount() == null ? 0 : v.getUsedCount()) + 1);
                    voucherRepository.save(v);
                       
                    voucherUserRepository.findByUser_IdAndVoucher_VoucherId(buyer.getId(), v.getVoucherId())
                        .ifPresent(vu -> {
                            vu.setQuantityUsed((vu.getQuantityUsed() == null ? 0 : vu.getQuantityUsed()) + 1);
                            voucherUserRepository.save(vu);
                        });
                           
                    boolean isSystemVoucher = v.getShop() != null && v.getShop().getRole() != null && "ADMIN".equalsIgnoreCase(v.getShop().getRole().getName());
                    for (int i = 0; i < savedOrders.size(); i++) {
                        Order savedOrder = savedOrders.get(i);
                        CheckoutPreviewResponse.ShopOrderPreview sp = previewRes.getShopOrders().get(i);
                        if (isSystemVoucher || savedOrder.getShop().getId() == v.getShop().getId()) { 
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

        // 5. Dọn dẹp giỏ hàng
        if (request.getCartItemIds() != null && !request.getCartItemIds().isEmpty()) {
            cartItemRepository.deleteAllById(request.getCartItemIds());
        }

        return responses;
    }

    private java.util.Map<User, List<OrderDetail>> groupItemsByShop(List<OrderItemRequest> items, boolean deductStock) {
        java.util.Map<User, List<OrderDetail>> mapShopToDetails = new java.util.HashMap<>();
        for (OrderItemRequest itemReq : items) {
            validateItemHasExactlyOneId(itemReq);
            User shopOwner;
            OrderDetail detail;

            if (itemReq.getProductId() != null) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
                if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
                User productShop = product.getShop();
                if (productShop == null || productShop.getStatus() != UserStatus.ACTIVE) {
                    throw new AppException(ErrorCode.SHOP_NOT_FOUND);
                }
                if (product.getStockQuantity() == null || product.getStockQuantity() < itemReq.getQuantity()) {
                    throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
                }
                if (deductStock) {
                    product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
                    productRepository.save(product);
                }
                shopOwner = productShop;
                detail = OrderDetail.builder()
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(resolveProductPrice(product))
                        .itemType("PRODUCT")
                        .isRequestedReturn(false)
                        .build();
            } else if (itemReq.getMysteryBoxId() != null) {
                MysteryBox box = mysteryBoxRepository.findById(itemReq.getMysteryBoxId())
                        .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));
                if (!Boolean.TRUE.equals(box.getIsActive())) throw new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND);
                User boxShop = box.getShop();
                if (boxShop == null || boxShop.getStatus() != UserStatus.ACTIVE) {
                    throw new AppException(ErrorCode.SHOP_NOT_FOUND);
                }
                int sold = box.getSoldQuantity() == null ? 0 : box.getSoldQuantity();
                int remaining = (box.getTotalQuantity() == null ? 0 : box.getTotalQuantity()) - sold;
                if (remaining < itemReq.getQuantity()) throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
                if (deductStock) {
                    box.setSoldQuantity(sold + itemReq.getQuantity());
                    mysteryBoxRepository.save(box);
                }
                shopOwner = boxShop;
                detail = OrderDetail.builder()
                        .mysteryBox(box)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(box.getPrice())
                        .itemType("MYSTERY_BOX")
                        .isRequestedReturn(false)
                        .build();
            } else {
                BuildCombo combo = buildComboRepository.findById(itemReq.getBuildComboId())
                        .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
                if (!"ACTIVE".equalsIgnoreCase(combo.getStatus())) throw new AppException(ErrorCode.COMBO_NOT_FOUND);
                User comboShop = combo.getShop();
                if (comboShop == null || comboShop.getStatus() != UserStatus.ACTIVE) {
                    throw new AppException(ErrorCode.SHOP_NOT_FOUND);
                }
                int sold = combo.getSoldQuantity() == null ? 0 : combo.getSoldQuantity();
                int remaining = (combo.getTotalQuantity() == null ? 0 : combo.getTotalQuantity()) - sold;
                if (remaining < itemReq.getQuantity()) throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
                if (deductStock) {
                    combo.setSoldQuantity(sold + itemReq.getQuantity());
                    buildComboRepository.save(combo);
                }
                shopOwner = comboShop;
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

        BigDecimal finalTotalAmount = totalGoodsAmount.add(totalShippingFee).subtract(totalVoucherDiscount);
        if (finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) finalTotalAmount = BigDecimal.ZERO;

        return CheckoutPreviewResponse.builder()
                .shopOrders(shopOrderPreviews)
                .totalGoodsAmount(totalGoodsAmount)
                .totalShippingFee(totalShippingFee)
                .totalVoucherDiscount(totalVoucherDiscount)
                .finalTotalAmount(finalTotalAmount)
                .build();
    }

    private BigDecimal applyVouchersAndReturnTotalDiscount(User buyer, List<String> voucherCodes, List<ShopOrderPreview> shopPreviews, BigDecimal totalGoodsAmount, BigDecimal totalShippingFee) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (voucherCodes == null || voucherCodes.isEmpty()) return totalDiscount;

        for (String code : voucherCodes) {
            Voucher voucher = voucherRepository.findByVoucherCode(code.toUpperCase()).orElse(null);
            if (voucher == null || !Boolean.TRUE.equals(voucher.getActive()) || voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
                continue; // Skip invalid vouchers
            }

            if (voucher.getUsageLimit() != null) {
                int used = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
                if (used >= voucher.getUsageLimit()) {
                    continue;
                }
            }

            if (voucher.getPerUserLimit() != null && buyer != null) {
                int userUsed = voucherUsageRepository.countByUserAndVoucher(buyer, voucher);
                if (userUsed >= voucher.getPerUserLimit()) {
                    continue;
                }
            }

            boolean isSystemVoucher = voucher.getShop() != null && voucher.getShop().getRole() != null && "ADMIN".equalsIgnoreCase(voucher.getShop().getRole().getName());
            if (isSystemVoucher) {
                if (voucher.getMinOrderValue() != null && totalGoodsAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                     continue; 
                }
                BigDecimal discount = calculateDiscount(voucher, totalGoodsAmount, totalShippingFee);
                totalDiscount = totalDiscount.add(discount);
                
                boolean isFreeShip = VoucherType.FREE_SHIPPING.equals(voucher.getVoucherType());
                BigDecimal baseTotal = isFreeShip ? totalShippingFee : totalGoodsAmount;

                for (ShopOrderPreview sp : shopPreviews) {
                    if (baseTotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal spBase = isFreeShip ? sp.getShippingFee() : sp.getGoodsAmount();
                        BigDecimal ratio = spBase.divide(baseTotal, 4, java.math.RoundingMode.HALF_UP);
                        BigDecimal shopVoucherShare = discount.multiply(ratio);
                        sp.setVoucherDiscount(sp.getVoucherDiscount().add(shopVoucherShare));
                        sp.getVoucherDiscountsDetail().put(code.toUpperCase(), shopVoucherShare);
                    }
                }
            } else {
                User shopOwner = voucher.getShop();
                ShopOrderPreview targetShopPreview = shopPreviews.stream()
                        .filter(sp -> sp.getShopId() == shopOwner.getId())
                        .findFirst()
                        .orElse(null);
                
                if (targetShopPreview != null) {
                    if (voucher.getMinOrderValue() != null && targetShopPreview.getGoodsAmount().compareTo(voucher.getMinOrderValue()) < 0) {
                         continue;
                    }
                    BigDecimal discount = calculateDiscount(voucher, targetShopPreview.getGoodsAmount(), targetShopPreview.getShippingFee());
                    targetShopPreview.setVoucherDiscount(targetShopPreview.getVoucherDiscount().add(discount));
                    targetShopPreview.getVoucherDiscountsDetail().put(code.toUpperCase(), discount);
                    totalDiscount = totalDiscount.add(discount);
                }
            }
        }
        return totalDiscount;
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal goodsAmount, BigDecimal shippingFee) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (VoucherType.PERCENTAGE.equals(voucher.getVoucherType())) {
            BigDecimal percentage = voucher.getDiscountValue().divide(BigDecimal.valueOf(100));
            discountAmount = goodsAmount.multiply(percentage);
            if (voucher.getMaxDiscount() != null && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                discountAmount = voucher.getMaxDiscount();
            }
        } else if (VoucherType.FIXED_AMOUNT.equals(voucher.getVoucherType())) {
            discountAmount = voucher.getDiscountValue();
            if (discountAmount.compareTo(goodsAmount) > 0) {
                discountAmount = goodsAmount;
            }
        } else if (VoucherType.FREE_SHIPPING.equals(voucher.getVoucherType())) {
            discountAmount = voucher.getDiscountValue();
            if (discountAmount.compareTo(shippingFee) > 0) {
                discountAmount = shippingFee;
            }
        }
        return discountAmount;
    }

    // ============================================================
    //  STATE MACHINE
    // ============================================================

    @Transactional
    @Override
    public OrderResponse markOrderAsConfirmed(int orderId) {
        Order order = findOrderById(orderId);
        // Flow: PENDING / PAID -> CONFIRMED
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            log.error("Invalid state transition to CONFIRMED for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.CONFIRMED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsReady(int orderId) {
        Order order = findOrderById(orderId);
        // Flow: CONFIRMED -> READY
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.error("Invalid state transition to READY for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.READY);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsShipping(int orderId) {
        Order order = findOrderById(orderId);
        // Flow: READY -> SHIPPING
        if (order.getStatus() != OrderStatus.READY) {
            log.error("Invalid state transition to SHIPPING for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.SHIPPING);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsDelivered(int orderId) {
        Order order = findOrderById(orderId);
        // Flow: SHIPPING -> DELIVERED
        if (order.getStatus() != OrderStatus.SHIPPING) {
            log.error("Invalid state transition to DELIVERED for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        order.setStatus(OrderStatus.DELIVERED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsCancelled(int orderId) {
        Order order = findOrderById(orderId);
        // Chỉ được huỷ khi đơn đang ở trạng thái có thể huỷ (PENDING, PAID, CONFIRMED, READY)
        // CANCELLED / FAILED / DELIVERED / COMPLETED đều không được huỷ lại (tránh hoàn kho 2 lần)
        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.READY) {
            log.error("Cannot cancel order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }

        // Trả lại kho (chỉ chạy 1 lần vì status đã được guard ở trên)
        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    @Override
    public OrderResponse markOrderAsFailed(int orderId) {
        Order order = findOrderById(orderId);
        // Chỉ đơn đang SHIPPING mới có thể báo giao thất bại
        if (order.getStatus() != OrderStatus.SHIPPING) {
            log.error("Invalid state transition to FAILED for order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
        
        // Giao hàng thất bại -> Hàng trả về shop -> Trả lại kho
        restoreStock(order);
        
        order.setStatus(OrderStatus.FAILED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    // ============================================================
    //  PRIVATE HELPERS
    // ============================================================

    private void restoreStock(Order order) {
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
        for (OrderDetail detail : details) {
            int qty = detail.getQuantity();
            if ("PRODUCT".equals(detail.getItemType()) && detail.getProduct() != null) {
                Product product = detail.getProduct();
                product.setStockQuantity(product.getStockQuantity() + qty);
                productRepository.save(product);
            } else if ("MYSTERY_BOX".equals(detail.getItemType()) && detail.getMysteryBox() != null) {
                MysteryBox box = detail.getMysteryBox();
                box.setSoldQuantity(Math.max(0, box.getSoldQuantity() - qty));
                mysteryBoxRepository.save(box);
            } else if ("BUILD_COMBO".equals(detail.getItemType()) && detail.getBuildCombo() != null) {
                BuildCombo combo = detail.getBuildCombo();
                combo.setSoldQuantity(Math.max(0, combo.getSoldQuantity() - qty));
                buildComboRepository.save(combo);
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
     * COD      => PENDING  (thanh toán khi nhận hàng, chờ Shop xác nhận)
     * TRANSFER => PAID     (đã thanh toán online, chờ Shop xác nhận)
     */
    private OrderStatus resolveInitialStatus(String paymentMethod) {
        if ("TRANSFER".equalsIgnoreCase(paymentMethod)) {
            return OrderStatus.PAID;
        }
        return OrderStatus.PENDING; // mặc định COD
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
     * Thủ công build OrderResponse khi createOrder (vì lúc này các detail chưa được flush vào DB)
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

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .buyerName(order.getBuyer() != null ? order.getBuyer().getFullName() : null)
                .shopName(order.getShop() != null ? order.getShop().getShopName() : null)
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
