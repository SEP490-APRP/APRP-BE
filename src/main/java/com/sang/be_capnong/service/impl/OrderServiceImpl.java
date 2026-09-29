package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.CreateOrderRequest;
import com.sang.be_capnong.dto.request.OrderItemRequest;
import com.sang.be_capnong.dto.response.OrderDetailResponse;
import com.sang.be_capnong.dto.response.OrderResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.OrderMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.OrderService;
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

    // ============================================================
    //  TẠO ĐƠN HÀNG
    // ============================================================

    /**
     * Workflow tạo đơn hàng:
     * 1. Lấy thông tin Buyer từ JWT token đang đăng nhập
     * 2. Validate từng item trong request (phải có đúng 1 trong 3 ID: productId / mysteryBoxId / buildComboId)
     * 3. Tính tổng tiền và lấy thông tin Shop từ item đầu tiên
     * 4. Tạo Order với status:
     *    - PENDING  nếu paymentMethod = "COD"     (thanh toán khi nhận hàng)
     *    - PAID     nếu paymentMethod = "TRANSFER" (đã thanh toán online)
     * 5. Tạo từng OrderDetail gắn vào Order
     * 6. Lưu vào DB và trả về OrderResponse
     */
    @Transactional
    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Lấy buyer từ token
        User buyer = getCurrentUser();

        // 2. Validate items không rỗng (đã validate ở @Valid, nhưng double-check thủ công)
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }

        // 3. Xây dựng danh sách OrderDetail và tính tổng tiền
        List<OrderDetail> orderDetails = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        User shopOwner = null; // sẽ lấy từ item đầu tiên

        for (OrderItemRequest itemReq : request.getItems()) {
            // Validate item phải có đúng 1 loại ID
            validateItemHasExactlyOneId(itemReq);

            if (itemReq.getProductId() != null) {
                // --- PRODUCT ---
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                // Kiểm tra tồn kho
                if (product.getStockQuantity() == null || product.getStockQuantity() < itemReq.getQuantity()) {
                    throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
                }

                // Trừ tồn kho
                product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
                productRepository.save(product);

                // Lấy giá thực tế (có áp dụng discount nếu còn hiệu lực)
                BigDecimal unitPrice = resolveProductPrice(product);

                // Gán shop chủ đơn từ product đầu tiên
                if (shopOwner == null) {
                    shopOwner = product.getShop();
                }

                // Build OrderDetail
                OrderDetail detail = OrderDetail.builder()
                        .product(product)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(unitPrice)
                        .itemType("PRODUCT")
                        .isRequestedReturn(false)
                        .build();
                orderDetails.add(detail);
                totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            } else if (itemReq.getMysteryBoxId() != null) {
                // --- MYSTERY BOX ---
                MysteryBox box = mysteryBoxRepository.findById(itemReq.getMysteryBoxId())
                        .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

                if (!Boolean.TRUE.equals(box.getIsActive())) {
                    throw new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND);
                }

                // Kiểm tra số lượng còn lại
                int sold = box.getSoldQuantity() == null ? 0 : box.getSoldQuantity();
                int remaining = (box.getTotalQuantity() == null ? 0 : box.getTotalQuantity()) - sold;
                if (remaining < itemReq.getQuantity()) {
                    throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
                }

                // Tăng soldQuantity
                box.setSoldQuantity(sold + itemReq.getQuantity());
                mysteryBoxRepository.save(box);

                if (shopOwner == null) {
                    shopOwner = box.getShop();
                }

                OrderDetail detail = OrderDetail.builder()
                        .mysteryBox(box)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(box.getPrice())
                        .itemType("MYSTERY_BOX")
                        .isRequestedReturn(false)
                        .build();
                orderDetails.add(detail);
                totalAmount = totalAmount.add(box.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            } else {
                // --- BUILD COMBO ---
                BuildCombo combo = buildComboRepository.findById(itemReq.getBuildComboId())
                        .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

                if (!"ACTIVE".equalsIgnoreCase(combo.getStatus())) {
                    throw new AppException(ErrorCode.COMBO_NOT_FOUND);
                }

                // Kiểm tra số lượng còn lại
                int sold = combo.getSoldQuantity() == null ? 0 : combo.getSoldQuantity();
                int remaining = (combo.getTotalQuantity() == null ? 0 : combo.getTotalQuantity()) - sold;
                if (remaining < itemReq.getQuantity()) {
                    throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
                }

                // Tăng soldQuantity
                combo.setSoldQuantity(sold + itemReq.getQuantity());
                buildComboRepository.save(combo);

                if (shopOwner == null) {
                    shopOwner = combo.getShop();
                }

                OrderDetail detail = OrderDetail.builder()
                        .buildCombo(combo)
                        .quantity(itemReq.getQuantity())
                        .unitPrice(combo.getPrice())
                        .itemType("BUILD_COMBO")
                        .isRequestedReturn(false)
                        .build();
                orderDetails.add(detail);
                totalAmount = totalAmount.add(combo.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            }
        }

        // 4. Xác định trạng thái đơn hàng theo phương thức thanh toán
        OrderStatus initialStatus = resolveInitialStatus(request.getPaymentMethod());

        // 5. Tạo Order
        Order order = Order.builder()
                .buyer(buyer)
                .shop(shopOwner)
                .recipientName(request.getRecipientName() != null ? request.getRecipientName() : buyer.getFullName())
                .recipientPhone(request.getRecipientPhone() != null ? request.getRecipientPhone() : buyer.getPhoneNumber())
                .shippingAddress(request.getShippingAddress())
                .note(request.getNote())
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .status(initialStatus)
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 6. Gắn order vào từng OrderDetail rồi lưu
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(savedOrder);
        }
        orderDetailRepository.saveAll(orderDetails);

        log.info("[CreateOrder] Buyer id={} created order id={} | status={} | total={}",
                buyer.getId(), savedOrder.getOrderId(), initialStatus, totalAmount);

        return buildOrderResponse(savedOrder, orderDetails);
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
        // Chỉ có thể huỷ trước khi SHIPPING
        if (order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.FAILED
                || order.getStatus() == OrderStatus.COMPLETED) {
            log.error("Cannot cancel order id {}. Current status: {}", orderId, order.getStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }
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
        order.setStatus(OrderStatus.FAILED);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    // ============================================================
    //  PRIVATE HELPERS
    // ============================================================

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
