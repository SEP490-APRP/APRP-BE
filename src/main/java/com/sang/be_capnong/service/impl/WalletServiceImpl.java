package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.response.wallet.OrderSettlementResponse;
import com.sang.be_capnong.dto.response.wallet.RefundDueResponse;
import com.sang.be_capnong.dto.response.wallet.WalletResponse;
import com.sang.be_capnong.dto.response.wallet.WalletTransactionResponse;
import com.sang.be_capnong.dto.request.wallet.WalletTopupCreateRequest;
import com.sang.be_capnong.dto.response.wallet.WalletTopupResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.*;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

/**
 * Quyết toán ví theo đặc tả CapNong:
 * - Phí dịch vụ nông dân: {farmerFeeRate} x (tiền hàng - voucher shop), trừ vào tiền Shop nhận.
 * - Phí dịch vụ shipper: {shipperFeeRate} x cước giao hàng, trừ vào tiền Shipper nhận.
 * - Voucher shop: Shop chịu. Voucher sàn (giảm tiền hàng / freeship): sàn bù, Shop và Shipper nhận đủ.
 * Chia tiền khi đơn COMPLETED. Bất biến: buyerPaid = shopReceive + shipperReceive + platformReceive + refundAmount.
 *
 * Tiền tạm giữ (frozenBalance): Shop + Sàn thấy phần của mình ngay khi tiền vào sàn
 * (PayOS PAID, COD lúc Shipper thu tiền mặt); Shipper thấy cước ngay khi nhận đơn.
 * Khi COMPLETED tiền tạm giữ được giữ lại 24h (trùng thời hạn hoàn hàng của Buyer) rồi mới gỡ và cộng vào số dư rút được;
 * huỷ / thất bại thì gỡ bỏ. Khi có khiếu nại/hoàn trả, tiền hoàn được chuyển vào ví Buyer và trừ tiền tạm giữ tương ứng của Shop.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    public static final String TYPE_SHOP = "SHOP";
    public static final String TYPE_SHIPPER = "SHIPPER";
    public static final String TYPE_PLATFORM = "PLATFORM";
    public static final String TYPE_BUYER = "BUYER";
    /** Ví giới thiệu của Buyer (đặc tả: Social Commerce & Referral Wallet) */
    public static final String TYPE_REFERRAL = "REFERRAL";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserRepository userRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final WalletTopupRepository walletTopupRepository;
    private final PaymentRepository paymentRepository;
    private final CheckoutBatchRepository checkoutBatchRepository;
    private final PayOS payOS;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Value("${capnong.wallet.min-topup:10000}")
    private BigDecimal minTopup;

    @Value("${capnong.wallet.max-topup:20000000}")
    private BigDecimal maxTopup;

    @Value("${payos.return-url:http://localhost:3000/payment/result}")
    private String defaultReturnUrl;

    @Value("${payos.cancel-url:http://localhost:3000/payment/result}")
    private String defaultCancelUrl;

    /** Số tiền rút tối thiểu mỗi lệnh */
    @Value("${capnong.wallet.min-withdraw:50000}")
    private BigDecimal minWithdraw;

    @Value("${capnong.wallet.farmer-fee-rate:0.10}")
    private BigDecimal farmerFeeRate;

    @Value("${capnong.wallet.shipper-fee-rate:0.03}")
    private BigDecimal shipperFeeRate;

    // ───────────────────────────── Quyết toán ─────────────────────────────

    @Override
    @Transactional
    public OrderSettlementResponse settleOrder(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getSettledAt() != null) {
            log.info("[Wallet] Order #{} already settled at {}, skip", orderId, order.getSettledAt());
            return buildSettlement(order);
        }
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.REFUNDED) {
            throw new AppException(ErrorCode.ORDER_NOT_SETTLEABLE);
        }

        // 1. Quy tắc giữ tiền shop 24h: Người mua có 24h sau khi nhận hàng để hoàn hàng
        // Tiền của shop phải tiếp tục giữ ở frozenBalance cho đến khi hết 24h kể từ deliveredAt.
        // Cho phép giải phóng quyết toán ngay nếu:
        // - Admin chủ động gọi quyết toán
        // - Người mua đã chủ động bấm "Đã nhận hàng" xác nhận hài lòng (completedAt != null) và không có khiếu nại
        boolean isExplicitAdmin = false;
        try {
            User me = currentUser();
            isExplicitAdmin = hasRole(me, "ADMIN");
        } catch (Exception ignored) {}

        boolean buyerConfirmed = order.getCompletedAt() != null;
        if (!isExplicitAdmin && !buyerConfirmed && order.getStatus() == OrderStatus.COMPLETED
                && order.getDeliveredAt() != null && LocalDateTime.now().isBefore(order.getDeliveredAt().plusHours(24))) {
            log.info("[Wallet] Order #{} delivered at {}. 24h return hold active until {}. Settle deferred.",
                    orderId, order.getDeliveredAt(), order.getDeliveredAt().plusHours(24));
            return buildSettlement(order);
        }

        // 2. Không quyết toán nếu đơn hàng có yêu cầu hoàn trả đang mở
        boolean hasActiveReturn = returnRequestRepository.findByOrderDetail_Order_OrderId(orderId).stream()
                .anyMatch(r -> r.getStatus() != ReturnStatus.COMPLETED
                        && r.getStatus() != ReturnStatus.REJECTED
                        && r.getStatus() != ReturnStatus.CANCELLED
                        && r.getStatus() != ReturnStatus.RETURN_FAILED);
        if (hasActiveReturn) {
            log.info("[Wallet] Order #{} has active return requests, settle deferred until returns complete", orderId);
            return buildSettlement(order);
        }

        Split s = computeSplit(order);
        String ref = "Đơn #" + orderId;

        releaseHolds(order, WalletTransactionType.HOLD_RELEASED, ref + ": đơn hoàn tất, chuyển tiền tạm giữ sang số dư");

        Wallet shopWallet = getOrCreateWallet(order.getShop(), TYPE_SHOP);
        credit(shopWallet, order, WalletTransactionType.SHOP_INCOME, s.shopReceive,
                ref + ": tiền hàng " + vnd(s.goods) + (nz(order.getRefundAmount()).signum() > 0 ? " (đã trừ hoàn trả " + vnd(order.getRefundAmount()) + ")" : "")
                        + " - voucher shop " + vnd(s.shopVoucher)
                        + " - phí dịch vụ " + pct(farmerFeeRate) + " " + vnd(s.farmerFee));

        if (order.getShipper() != null && s.shipperReceive.signum() != 0 && order.getShipperPaidAt() == null) {
            Wallet shipperWallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
            credit(shipperWallet, order, WalletTransactionType.SHIPPER_INCOME, s.shipperReceive,
                    ref + ": cước giao " + vnd(s.shippingFee) + " - phí dịch vụ " + pct(shipperFeeRate) + " " + vnd(s.shipperFee));
            order.setShipperPaidAt(LocalDateTime.now());
        }

        Wallet platform = getPlatformWalletEntity();
        credit(platform, order, WalletTransactionType.PLATFORM_FARMER_FEE, s.farmerFee,
                ref + ": phí dịch vụ nông dân " + pct(farmerFeeRate));
        credit(platform, order, WalletTransactionType.PLATFORM_SHIPPER_FEE, s.shipperFee,
                ref + ": phí dịch vụ shipper " + pct(shipperFeeRate));
        credit(platform, order, WalletTransactionType.PLATFORM_FREESHIP_SUBSIDY, s.platformFreeship.negate(),
                ref + ": sàn bù voucher freeship");
        credit(platform, order, WalletTransactionType.PLATFORM_VOUCHER_SUBSIDY, s.platformVoucherSubsidy.negate(),
                ref + ": sàn bù voucher giảm tiền hàng");

        order.setSettledAt(LocalDateTime.now());
        if ("COD".equalsIgnoreCase(order.getPaymentMethod()) && order.getCodPaymentStatus() == null) {
            order.setCodPaymentStatus(CodPaymentStatus.CONFIRMED);
        }
        if (order.getShipper() != null && order.getShipperPaidAt() == null) {
            order.setShipperPaidAt(order.getSettledAt());
        }
        orderRepository.save(order);

        // Bắn sự kiện quyết toán đơn hàng để lắng nghe AFTER_COMMIT chia hoa hồng tiếp thị liên kết
        eventPublisher.publishEvent(new com.sang.be_capnong.event.OrderSettledEvent(order.getOrderId()));

        log.info("[Wallet] Settled order #{}: buyerPaid={}, shop={}, shipper={}, platform={}, refundAmount={}",
                orderId, s.buyerPaid, s.shopReceive, s.shipperReceive, s.platformReceive, order.getRefundAmount());
        return buildSettlement(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSettlementResponse getOrderSettlement(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        User me = currentUser();
        boolean admin = hasRole(me, "ADMIN") || hasRole(me, "STAFF");
        boolean isShop = order.getShop() != null && order.getShop().getId() == me.getId();
        boolean isShipper = order.getShipper() != null && order.getShipper().getId() == me.getId();
        if (!admin && !isShop && !isShipper) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return buildSettlement(order);
    }

    // ───────────────────────────── Tiền tạm giữ ─────────────────────────────

    @Override
    @Transactional
    public void holdShopAndPlatformIncome(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getSettledAt() != null || order.getShop() == null) {
            return;
        }
        Split s = computeSplit(order, true);
        String ref = "Đơn #" + orderId;
        hold(getOrCreateWallet(order.getShop(), TYPE_SHOP), order, WalletTransactionType.SHOP_INCOME_HOLD, s.shopReceive,
                ref + ": tạm giữ tiền hàng, nhận khi đơn hoàn tất");
        hold(getPlatformWalletEntity(), order, WalletTransactionType.PLATFORM_INCOME_HOLD, s.platformReceive,
                ref + ": tạm giữ phần sàn, nhận khi đơn hoàn tất");
    }

    @Override
    @Transactional
    public void holdShipperIncome(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getSettledAt() != null || order.getShipper() == null) {
            return;
        }
        Split s = computeSplit(order, true);
        hold(getOrCreateWallet(order.getShipper(), TYPE_SHIPPER), order, WalletTransactionType.SHIPPER_INCOME_HOLD, s.shipperReceive,
                "Đơn #" + orderId + ": tạm giữ cước giao, nhận khi đơn hoàn tất");
    }

    @Override
    @Transactional
    public void cancelHolds(int orderId, String reason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getSettledAt() != null) {
            return;
        }
        releaseHolds(order, WalletTransactionType.HOLD_CANCELLED, "Đơn #" + orderId + ": " + reason + ", gỡ tiền tạm giữ");
    }

    // ───────────────────────────── Hoàn tiền / Hoàn hàng ─────────────────────────────

    @Override
    @Transactional
    public void processReturnRefund(ReturnRequest returnRequest) {
        if (returnRequest == null || returnRequest.getOrderDetail() == null) {
            return;
        }
        Order order = returnRequest.getOrderDetail().getOrder();
        if (order == null) {
            return;
        }

        BigDecimal refundAmt = money(nz(returnRequest.getRefundAmount()));
        if (refundAmt.signum() <= 0) {
            return;
        }

        User buyer = order.getBuyer();
        if (buyer == null) {
            log.error("[Wallet] Cannot refund return #{}: order #{} has no buyer", returnRequest.getReturnId(), order.getOrderId());
            return;
        }

        // 1. Hoàn tiền vào ví người mua (cộng totalBalance)
        Wallet buyerWallet = getOrCreateWallet(buyer, TYPE_BUYER);
        String buyerDesc = "Hoàn tiền yêu cầu #" + returnRequest.getReturnId() + " (Đơn #" + order.getOrderId() + "): +" + vnd(refundAmt);
        credit(buyerWallet, order, WalletTransactionType.BUYER_REFUND, refundAmt, buyerDesc);

        // 2. Tính phần khấu trừ của Shop và Sàn từ số tiền hoàn này
        BigDecimal shopDeduct = money(refundAmt.multiply(BigDecimal.ONE.subtract(farmerFeeRate)));
        BigDecimal platformDeduct = refundAmt.subtract(shopDeduct);

        // Nếu đơn chưa quyết toán (đang giữ trong frozenBalance): trừ frozenBalance của Shop và Sàn
        if (order.getSettledAt() == null) {
            if (order.getShop() != null && shopDeduct.signum() > 0) {
                Wallet shopWallet = getOrCreateWallet(order.getShop(), TYPE_SHOP);
                deductHoldForRefund(shopWallet, order, WalletTransactionType.SHOP_REFUND_DEDUCT, shopDeduct,
                        "Đơn #" + order.getOrderId() + ": Trừ tiền tạm giữ do hoàn trả yêu cầu #" + returnRequest.getReturnId() + " (" + vnd(shopDeduct) + ")");
            }
            if (platformDeduct.signum() > 0) {
                Wallet platform = getPlatformWalletEntity();
                deductHoldForRefund(platform, order, WalletTransactionType.PLATFORM_REFUND_DEDUCT, platformDeduct,
                        "Đơn #" + order.getOrderId() + ": Trừ tiền tạm giữ sàn do hoàn trả yêu cầu #" + returnRequest.getReturnId() + " (" + vnd(platformDeduct) + ")");
            }
        } else {
            // Trường hợp hy hữu đơn đã quyết toán: trừ trực tiếp vào số dư totalBalance của Shop và Sàn
            if (order.getShop() != null && shopDeduct.signum() > 0) {
                Wallet shopWallet = getOrCreateWallet(order.getShop(), TYPE_SHOP);
                credit(shopWallet, order, WalletTransactionType.SHOP_REFUND_DEDUCT, shopDeduct.negate(),
                        "Đơn #" + order.getOrderId() + ": Thu hồi tiền hàng do hoàn trả yêu cầu #" + returnRequest.getReturnId() + " (" + vnd(shopDeduct) + ")");
            }
            if (platformDeduct.signum() > 0) {
                Wallet platform = getPlatformWalletEntity();
                credit(platform, order, WalletTransactionType.PLATFORM_REFUND_DEDUCT, platformDeduct.negate(),
                        "Đơn #" + order.getOrderId() + ": Giảm doanh thu sàn do hoàn trả yêu cầu #" + returnRequest.getReturnId() + " (" + vnd(platformDeduct) + ")");
            }
        }

        // 3. Xử lý trả cước cho Shipper giao hàng hoàn (nếu có shipper và có cước hoàn)
        if (returnRequest.getShipper() != null && returnRequest.getReturnShippingFee() != null
                && returnRequest.getReturnShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal returnShipFee = money(returnRequest.getReturnShippingFee());
            BigDecimal shipperFeeDeduct = money(returnShipFee.multiply(shipperFeeRate));
            BigDecimal shipperIncome = returnShipFee.subtract(shipperFeeDeduct);

            Wallet shipperWallet = getOrCreateWallet(returnRequest.getShipper(), TYPE_SHIPPER);
            Wallet platform = getPlatformWalletEntity();

            if ("WALLET".equalsIgnoreCase(returnRequest.getShippingPaymentMethod())) {
                // Shop đã thanh toán cước hoàn từ ví sàn trước đó -> Sàn trả tiền cước vào ví cho Shipper
                credit(shipperWallet, order, WalletTransactionType.SHIPPER_INCOME, shipperIncome,
                        "Cước giao hàng hoàn trả yêu cầu #" + returnRequest.getReturnId() + " (Đơn #" + order.getOrderId() + "): +" + vnd(shipperIncome));
                credit(platform, order, WalletTransactionType.PLATFORM_SHIPPER_FEE, shipperFeeDeduct,
                        "Phí dịch vụ shipper giao hàng hoàn #" + returnRequest.getReturnId() + " (" + pct(shipperFeeRate) + "): +" + vnd(shipperFeeDeduct));
            } else {
                // Shop thanh toán COD -> Shop đã trả tiền mặt trực tiếp cho Shipper khi shipper giao hàng hoàn tới shop.
                // Shipper đã nhận tiền mặt từ Shop, sàn chỉ thu phí dịch vụ shipper
                credit(shipperWallet, order, WalletTransactionType.PLATFORM_SHIPPER_FEE, shipperFeeDeduct.negate(),
                        "Phí dịch vụ cước hoàn COD nhận tiền mặt yêu cầu #" + returnRequest.getReturnId() + " (" + pct(shipperFeeRate) + "): -" + vnd(shipperFeeDeduct));
                credit(platform, order, WalletTransactionType.PLATFORM_SHIPPER_FEE, shipperFeeDeduct,
                        "Phí dịch vụ shipper giao hàng hoàn COD #" + returnRequest.getReturnId() + " (" + pct(shipperFeeRate) + "): +" + vnd(shipperFeeDeduct));
            }
        }

        log.info("[Wallet] Processed return refund #{}: refunded {} to buyer #{}, deducted {} from shop #{}",
                returnRequest.getReturnId(), refundAmt, buyer.getId(), shopDeduct);
    }

    @Override
    @Transactional
    public void deductShopReturnShippingFee(ReturnRequest returnRequest) {
        if (returnRequest == null) return;
        Order order = returnRequest.getOrderDetail() != null ? returnRequest.getOrderDetail().getOrder() : null;
        User shop = returnRequest.getShop();
        if (shop == null) return;

        BigDecimal shipFee = money(nz(returnRequest.getReturnShippingFee()));
        if (shipFee.signum() <= 0) return;

        Wallet shopWallet = getOrCreateWallet(shop, TYPE_SHOP);
        Wallet locked = walletRepository.findByIdForUpdate(shopWallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // KIỂM TRA SỐ DƯ KHẢ DỤNG
        if (nz(locked.getTotalBalance()).compareTo(shipFee) < 0) {
            log.warn("[Wallet] Shop #{} has insufficient balance to pay return shipping fee: balance={}, required={}",
                    shop.getId(), locked.getTotalBalance(), shipFee);
            throw new AppException(ErrorCode.RETURN_WALLET_INSUFFICIENT_BALANCE);
        }

        // Khấu trừ số dư ví khả dụng của Shop
        BigDecimal newBalance = nz(locked.getTotalBalance()).subtract(shipFee);
        locked.setTotalBalance(newBalance);
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .order(order)
                .type(WalletTransactionType.SHOP_RETURN_SHIPPING_DEDUCT)
                .amount(shipFee.negate())
                .balanceAfter(newBalance)
                .frozenAfter(nz(locked.getFrozenBalance()))
                .description("Yêu cầu hoàn #" + returnRequest.getReturnId() + " (Đơn #" + (order != null ? order.getOrderId() : "") + "): Shop thanh toán cước ship hoàn -" + vnd(shipFee))
                .createdAt(LocalDateTime.now())
                .build());

        log.info("[Wallet] Successfully deducted return shipping fee {} from shop #{} wallet. New balance={}",
                shipFee, shop.getId(), newBalance);
    }

    @Override
    @Transactional
    public void refundShopReturnShippingFee(ReturnRequest returnRequest) {
        if (returnRequest == null || !Boolean.TRUE.equals(returnRequest.getIsShippingFeePaid())) {
            return;
        }
        if (!"WALLET".equalsIgnoreCase(returnRequest.getShippingPaymentMethod())) {
            return;
        }
        Order order = returnRequest.getOrderDetail() != null ? returnRequest.getOrderDetail().getOrder() : null;
        User shop = returnRequest.getShop();
        if (shop == null) return;

        BigDecimal shipFee = money(nz(returnRequest.getReturnShippingFee()));
        if (shipFee.signum() <= 0) return;

        Wallet shopWallet = getOrCreateWallet(shop, TYPE_SHOP);
        credit(shopWallet, order, WalletTransactionType.SHOP_RETURN_SHIPPING_REFUND, shipFee,
                "Hoàn lại cước ship hoàn cho Shop do yêu cầu #" + returnRequest.getReturnId() + " bị huỷ/từ chối: +" + vnd(shipFee));
        returnRequest.setIsShippingFeePaid(false);
        log.info("[Wallet] Refunded return shipping fee {} back to shop #{}", shipFee, shop.getId());
    }

    private static final Set<WalletTransactionType> HOLD_TYPES = EnumSet.of(
            WalletTransactionType.SHOP_INCOME_HOLD, WalletTransactionType.SHIPPER_INCOME_HOLD,
            WalletTransactionType.PLATFORM_INCOME_HOLD, WalletTransactionType.HOLD_RELEASED,
            WalletTransactionType.HOLD_CANCELLED,
            WalletTransactionType.SHOP_REFUND_DEDUCT, WalletTransactionType.PLATFORM_REFUND_DEDUCT);

    /** Gỡ phần tiền tạm giữ còn lại của đơn ở từng ví (mỗi ví 1 bút toán) */
    private void releaseHolds(Order order, WalletTransactionType type, String description) {
        Map<Integer, BigDecimal> netByWallet = new LinkedHashMap<>();
        Map<Integer, Wallet> wallets = new HashMap<>();
        for (WalletTransaction t : walletTransactionRepository.findByOrder_OrderIdOrderByIdAsc(order.getOrderId())) {
            if (!HOLD_TYPES.contains(t.getType())) continue;
            int wid = t.getWallet().getWalletId();
            wallets.putIfAbsent(wid, t.getWallet());
            netByWallet.merge(wid, nz(t.getAmount()), BigDecimal::add);
        }
        netByWallet.forEach((wid, net) -> hold(wallets.get(wid), order, type, net.negate(), description));
    }

    /** Cộng/trừ tiền tạm giữ và ghi sổ cái. Mỗi (ví, đơn, loại) chỉ 1 lần. */
    private void hold(Wallet wallet, Order order, WalletTransactionType type, BigDecimal amount, String description) {
        if (amount == null || amount.signum() == 0) {
            return;
        }
        if (walletTransactionRepository.existsByWallet_WalletIdAndOrder_OrderIdAndType(wallet.getWalletId(), order.getOrderId(), type)) {
            return;
        }
        Wallet locked = walletRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        BigDecimal newFrozen = nz(locked.getFrozenBalance()).add(amount).max(BigDecimal.ZERO);
        locked.setFrozenBalance(newFrozen);
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .order(order)
                .type(type)
                .amount(amount)
                .balanceAfter(nz(locked.getTotalBalance()))
                .frozenAfter(newFrozen)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** Khấu trừ trực tiếp từ frozenBalance cho các bút toán hoàn hàng */
    private void deductHoldForRefund(Wallet wallet, Order order, WalletTransactionType type, BigDecimal amount, String description) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        Wallet locked = walletRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        BigDecimal newFrozen = nz(locked.getFrozenBalance()).subtract(amount).max(BigDecimal.ZERO);
        locked.setFrozenBalance(newFrozen);
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .order(order)
                .type(type)
                .amount(amount.negate())
                .balanceAfter(nz(locked.getTotalBalance()))
                .frozenAfter(newFrozen)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ───────────────────────────── COD ─────────────────────────────

    @Override
    @Transactional
    public void recordCodCashCollected(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod()) || order.getShipper() == null) {
            return;
        }
        if (order.getCodCashConfirmedAt() != null) {
            return; // đã ghi nhận
        }
        BigDecimal cash = nz(order.getTotalAmount());
        Wallet shipperWallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
        order.setCodAmount(cash);
        order.setCodCashConfirmedAt(LocalDateTime.now());

        if (nz(order.getCodReservedAmount()).signum() > 0) {
            // Ký quỹ: tiền mặt thu của khách chính là tiền Shipper đã ứng -> trừ ví, không còn nợ sàn
            releaseCodReserveInternal(order, "giao thành công");
            credit(shipperWallet, order, WalletTransactionType.COD_CASH_COLLECTED, cash.negate(),
                    "Đơn #" + orderId + ": thu tiền mặt COD " + vnd(cash) + " của khách, trừ vào tiền ký quỹ");
            order.setCodSettled(true);
            order.setCodSettledAt(LocalDateTime.now());
            order.setCodPaymentStatus(CodPaymentStatus.SETTLED);
            orderRepository.save(order);
            return;
        }

        // Đơn nhận theo luồng cũ (chưa ký quỹ): ghi nợ, chờ Admin xác nhận nộp tiền
        order.setCodSettled(false);
        order.setCodPaymentStatus(CodPaymentStatus.CONFIRMED);
        orderRepository.save(order);
        credit(shipperWallet, order, WalletTransactionType.COD_CASH_COLLECTED, cash.negate(),
                "Đơn #" + orderId + ": giữ tiền mặt COD " + vnd(cash) + " của khách, cần nộp về sàn");
    }

    @Override
    @Transactional
    public void releaseShipperFeeOnDelivered(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getShipper() == null || order.getShipperPaidAt() != null) {
            return;
        }

        Split s = computeSplit(order);
        if (s.shipperReceive.signum() <= 0) {
            return;
        }

        Wallet shipperWallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
        String ref = "Đơn #" + orderId;

        // 1. Gỡ tạm giữ cước giao của Shipper (SHIPPER_INCOME_HOLD -> HOLD_RELEASED)
        hold(shipperWallet, order, WalletTransactionType.HOLD_RELEASED, s.shipperReceive.negate(),
                ref + ": giao thành công, chuyển cước giao tạm giữ sang số dư");

        // 2. Cộng cước giao vào số dư khả dụng của Shipper
        credit(shipperWallet, order, WalletTransactionType.SHIPPER_INCOME, s.shipperReceive,
                ref + ": cước giao " + vnd(s.shippingFee) + " - phí dịch vụ " + pct(shipperFeeRate) + " " + vnd(s.shipperFee));

        order.setShipperPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("[Wallet] Released shipper fee {} to shipper #{} upon delivery for order #{}",
                s.shipperReceive, order.getShipper().getId(), orderId);
    }

    // ───────────────────────────── Ký quỹ COD ─────────────────────────────

    @Override
    @Transactional
    public void reserveForShipperAccept(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getShipper() == null) {
            return;
        }
        Wallet wallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
        Wallet locked = walletRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // Ví âm = còn nợ tiền COD của luồng cũ -> không cho nhận thêm đơn nào
        if (nz(locked.getTotalBalance()).signum() < 0) {
            throw new AppException(ErrorCode.WALLET_HAS_DEBT, null,
                    Map.of("balance", nz(locked.getTotalBalance())));
        }
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return;
        }
        BigDecimal required = money(order.getTotalAmount());
        BigDecimal available = nz(locked.getTotalBalance()).subtract(nz(locked.getCodReserved())).max(BigDecimal.ZERO);
        if (available.compareTo(required) < 0) {
            throw new AppException(ErrorCode.SHIPPER_COD_BALANCE_INSUFFICIENT, null, Map.of(
                    "required", required,
                    "available", available,
                    "missing", required.subtract(available)));
        }
        order.setCodReservedAmount(required);
        order.setCodPaymentStatus(CodPaymentStatus.PENDING);
        orderRepository.save(order);
        reserve(locked, order, WalletTransactionType.COD_RESERVED, required,
                "Đơn #" + orderId + ": ký quỹ " + vnd(required) + " tiền COD, trả lại khi giao xong hoặc đơn không giao được");
    }

    @Override
    @Transactional
    public void releaseCodReserve(int orderId, String reason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        releaseCodReserveInternal(order, reason);
    }

    private void releaseCodReserveInternal(Order order, String reason) {
        BigDecimal amount = nz(order.getCodReservedAmount());
        if (amount.signum() <= 0 || order.getShipper() == null) {
            return;
        }
        Wallet wallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
        if (!walletTransactionRepository.existsByWallet_WalletIdAndOrder_OrderIdAndType(
                wallet.getWalletId(), order.getOrderId(), WalletTransactionType.COD_RESERVED)) {
            return;
        }
        reserve(wallet, order, WalletTransactionType.COD_RESERVE_RELEASED, amount.negate(),
                "Đơn #" + order.getOrderId() + ": " + reason + ", gỡ ký quỹ " + vnd(amount));
    }

    /**
     * Cộng (delta > 0) / trừ (delta < 0) tiền giữ chỗ COD. Số dư không đổi.
     * Sổ cái ghi amount = -delta (âm = số dư khả dụng giảm). Mỗi (ví, đơn, loại) chỉ 1 lần.
     */
    private void reserve(Wallet wallet, Order order, WalletTransactionType type, BigDecimal delta, String description) {
        if (delta == null || delta.signum() == 0) {
            return;
        }
        if (walletTransactionRepository.existsByWallet_WalletIdAndOrder_OrderIdAndType(wallet.getWalletId(), order.getOrderId(), type)) {
            return;
        }
        Wallet locked = walletRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        locked.setCodReserved(nz(locked.getCodReserved()).add(delta).max(BigDecimal.ZERO));
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .order(order)
                .type(type)
                .amount(delta.negate())
                .balanceAfter(nz(locked.getTotalBalance()))
                .frozenAfter(nz(locked.getFrozenBalance()))
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getShipperAvailableBalance(int shipperId) {
        return walletRepository.findFirstByUser_IdAndType(shipperId, TYPE_SHIPPER)
                .map(w -> nz(w.getTotalBalance()).subtract(nz(w.getCodReserved())))
                .orElse(BigDecimal.ZERO);
    }

    // ───────────────────────────── Nạp ví (PayOS) ─────────────────────────────

    @Override
    @Transactional
    public WalletTopupResponse createTopup(WalletTopupCreateRequest req) {
        BigDecimal amount = req == null ? null : req.getAmount();
        if (amount == null || amount.stripTrailingZeros().scale() > 0
                || amount.compareTo(minTopup) < 0 || amount.compareTo(maxTopup) > 0) {
            throw new AppException(ErrorCode.TOPUP_AMOUNT_INVALID,
                    "Số tiền nạp phải là số nguyên từ " + vnd(minTopup) + " đến " + vnd(maxTopup));
        }
        amount = money(amount);
        User me = currentUser();
        if (!hasRole(me, "SHIPPER") && !hasRole(me, "SHOP")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        Wallet wallet = walletRepository.findById(getMyWallet().getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        long code = nextTopupCode();
        String returnUrl = req.getReturnUrl() != null && !req.getReturnUrl().isBlank() ? req.getReturnUrl() : defaultReturnUrl;
        String cancelUrl = req.getCancelUrl() != null && !req.getCancelUrl().isBlank() ? req.getCancelUrl() : defaultCancelUrl;
        String checkoutUrl;
        try {
            CreatePaymentLinkResponse res = payOS.paymentRequests().create(CreatePaymentLinkRequest.builder()
                    .orderCode(code)
                    .amount(amount.longValueExact())
                    .description("Nap vi CapNong")
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .expiredAt(java.time.Instant.now().plusSeconds(30 * 60L).getEpochSecond())
                    .build());
            checkoutUrl = res.getCheckoutUrl();
        } catch (Exception e) {
            log.error("[Topup] Cannot create PayOS link for wallet {}: {}", wallet.getWalletId(), e.getMessage(), e);
            throw new AppException(ErrorCode.PAYMENT_FAILED);
        }
        WalletTopup t = walletTopupRepository.save(WalletTopup.builder()
                .wallet(wallet)
                .amount(amount)
                .payosOrderCode(String.valueOf(code))
                .checkoutUrl(checkoutUrl)
                .status(WalletTopupStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
        log.info("[Topup] #{} created: wallet {} amount {} code {}", t.getId(), wallet.getWalletId(), amount, code);
        return toTopupResponse(t);
    }

    @Override
    @Transactional
    public List<WalletTopupResponse> getMyTopups() {
        return walletTopupRepository.findTop20ByWallet_WalletIdOrderByCreatedAtDesc(getMyWallet().getWalletId())
                .stream().map(this::toTopupResponse).toList();
    }

    @Override
    @Transactional
    public WalletTopupResponse confirmTopup(String orderCode) {
        WalletTopup t = walletTopupRepository.findByPayosOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.TOPUP_NOT_FOUND));
        User me = currentUser();
        if (t.getWallet().getUser().getId() != me.getId() && !hasRole(me, "ADMIN")) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (t.getStatus() == WalletTopupStatus.PENDING) {
            try {
                PaymentLink link = payOS.paymentRequests().get(Long.parseLong(orderCode));
                if (link != null && PaymentLinkStatus.PAID.equals(link.getStatus())) {
                    settleTopupIfExists(orderCode);
                } else if (link != null && (PaymentLinkStatus.CANCELLED.equals(link.getStatus())
                        || PaymentLinkStatus.EXPIRED.equals(link.getStatus()))) {
                    t.setStatus(WalletTopupStatus.CANCELLED);
                    walletTopupRepository.save(t);
                }
            } catch (Exception e) {
                log.warn("[Topup] Cannot check PayOS link {}: {}", orderCode, e.getMessage());
            }
        }
        return toTopupResponse(t);
    }

    @Override
    @Transactional
    public boolean settleTopupIfExists(String orderCode) {
        if (orderCode == null || !walletTopupRepository.existsByPayosOrderCode(orderCode)) {
            return false;
        }
        WalletTopup t = walletTopupRepository.findByPayosOrderCodeForUpdate(orderCode).orElseThrow();
        if (t.getStatus() == WalletTopupStatus.PAID) {
            return true;
        }
        // Kể cả lệnh đã bị đánh dấu huỷ: tiền đã vào sàn thì vẫn cộng ví
        credit(t.getWallet(), null, WalletTransactionType.TOP_UP, t.getAmount(),
                "Nạp ví #" + t.getId() + ": " + vnd(t.getAmount()) + " qua PayOS");
        t.setStatus(WalletTopupStatus.PAID);
        t.setPaidAt(LocalDateTime.now());
        walletTopupRepository.save(t);
        log.info("[Topup] #{} PAID: +{} to wallet {}", t.getId(), t.getAmount(), t.getWallet().getWalletId());
        return true;
    }

    private long nextTopupCode() {
        for (int i = 0; i < 5; i++) {
            long code = System.currentTimeMillis() * 1000L + java.util.concurrent.ThreadLocalRandom.current().nextInt(1000);
            String c = String.valueOf(code);
            if (!walletTopupRepository.existsByPayosOrderCode(c)
                    && paymentRepository.findByPayosOrderCode(c).isEmpty()
                    && checkoutBatchRepository.findByPayosOrderCode(c).isEmpty()) {
                return code;
            }
        }
        throw new AppException(ErrorCode.PAYMENT_FAILED);
    }

    private WalletTopupResponse toTopupResponse(WalletTopup t) {
        return WalletTopupResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .orderCode(t.getPayosOrderCode())
                .checkoutUrl(t.getStatus() == WalletTopupStatus.PENDING ? t.getCheckoutUrl() : null)
                .createdAt(t.getCreatedAt())
                .paidAt(t.getPaidAt())
                .build();
    }

    @Override
    @Transactional
    public OrderSettlementResponse settleCodCash(int orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getCodCashConfirmedAt() == null || order.getShipper() == null) {
            throw new AppException(ErrorCode.COD_NOT_COLLECTED);
        }
        if (Boolean.TRUE.equals(order.getCodSettled())) {
            throw new AppException(ErrorCode.COD_ALREADY_SETTLED);
        }
        BigDecimal cash = nz(order.getCodAmount());
        Wallet shipperWallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
        credit(shipperWallet, order, WalletTransactionType.COD_CASH_REMITTED, cash,
                "Đơn #" + orderId + ": đã nộp tiền mặt COD " + vnd(cash) + " về sàn");

        order.setCodSettled(true);
        order.setCodSettledAt(LocalDateTime.now());
        order.setCodPaymentStatus(CodPaymentStatus.SETTLED);
        orderRepository.save(order);
        return buildSettlement(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSettlementResponse> getPendingCodOrders() {
        return orderRepository.findByCodCashConfirmedAtIsNotNullAndCodSettledFalseOrderByCodCashConfirmedAtAsc()
                .stream().map(this::buildSettlement).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundDueResponse> getRefundsDue() {
        return orderRepository.findByRefundAmountGreaterThanOrderByCreatedAtDesc(BigDecimal.ZERO).stream()
                .map(o -> RefundDueResponse.builder()
                        .orderId(o.getOrderId())
                        .buyerId(o.getBuyer() != null ? o.getBuyer().getId() : null)
                        .buyerName(o.getBuyer() != null ? o.getBuyer().getFullName() : null)
                        .buyerEmail(o.getBuyer() != null ? o.getBuyer().getEmail() : null)
                        .orderStatus(o.getStatus() != null ? o.getStatus().name() : null)
                        .paymentMethod(o.getPaymentMethod())
                        .totalAmount(o.getTotalAmount())
                        .refundAmount(o.getRefundAmount())
                        .refundReason(o.getRefundReason())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();
    }

    // ───────────────────────────── Rút tiền ─────────────────────────────

    @Override
    @Transactional
    public com.sang.be_capnong.dto.response.wallet.WithdrawResponse requestWithdraw(
            com.sang.be_capnong.dto.request.wallet.WithdrawCreateRequest req) {
        if (req == null) throw new AppException(ErrorCode.WITHDRAW_INVALID_BANK);
        String bank = trimToNull(req.getBankName());
        String number = req.getBankAccountNumber() == null ? null : req.getBankAccountNumber().replaceAll("\\s", "");
        String holder = trimToNull(req.getBankAccountHolder());
        if (bank == null || holder == null || number == null || !number.matches("\\d{6,20}")) {
            throw new AppException(ErrorCode.WITHDRAW_INVALID_BANK);
        }
        BigDecimal amount = money(req.getAmount());
        if (amount.compareTo(minWithdraw) < 0) {
            throw new AppException(ErrorCode.WITHDRAW_MIN_AMOUNT);
        }

        User me = currentUser();
        if (hasRole(me, "ADMIN")) throw new AppException(ErrorCode.UNAUTHORIZED);
        WalletResponse mine = getMyWallet();
        Wallet locked = walletRepository.findByIdForUpdate(mine.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        // Tiền đang ký quỹ COD không được rút
        if (nz(locked.getTotalBalance()).subtract(nz(locked.getCodReserved())).compareTo(amount) < 0) {
            throw new AppException(ErrorCode.WITHDRAW_INSUFFICIENT_BALANCE);
        }

        WithdrawRequest r = withdrawRequestRepository.save(WithdrawRequest.builder()
                .wallet(locked)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .receiveAmount(amount)
                .status(WithdrawStatus.PENDING)
                .bankName(bank)
                .bankAccountNumber(number)
                .bankAccountHolder(holder.toUpperCase(Locale.ROOT))
                .reason(trimToNull(req.getNote()))
                .createdAt(LocalDateTime.now())
                .build());
        credit(locked, null, WalletTransactionType.WITHDRAW_REQUESTED, amount.negate(),
                "Lệnh rút #" + r.getRequestId() + ": rút " + vnd(amount) + " về " + bank + " " + maskAccount(number));
        log.info("[Withdraw] #{} created by user {} wallet {} amount {}", r.getRequestId(), me.getId(), locked.getWalletId(), amount);
        return toWithdrawResponse(r);
    }

    @Override
    @Transactional
    public List<com.sang.be_capnong.dto.response.wallet.WithdrawResponse> getMyWithdrawals() {
        WalletResponse mine = getMyWallet();
        return withdrawRequestRepository.findByWallet_WalletIdOrderByCreatedAtDesc(mine.getWalletId())
                .stream().map(this::toWithdrawResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.sang.be_capnong.dto.response.wallet.WithdrawResponse> getWithdrawals(String status) {
        List<WithdrawRequest> list;
        if (status == null || status.isBlank()) {
            list = withdrawRequestRepository.findAllByOrderByCreatedAtDesc();
        } else {
            try {
                WithdrawStatus st = WithdrawStatus.valueOf(status.trim().toUpperCase());
                list = withdrawRequestRepository.findByStatusOrderByCreatedAtDesc(st);
            } catch (IllegalArgumentException e) {
                list = List.of();
            }
        }
        return list.stream().map(this::toWithdrawResponse).toList();
    }

    @Override
    @Transactional
    public com.sang.be_capnong.dto.response.wallet.WithdrawResponse approveWithdraw(int requestId, String note) {
        WithdrawRequest r = lockWithdrawPending(requestId);
        finishWithdraw(r, WithdrawStatus.COMPLETED, note);

        Wallet w = r.getWallet();
        w.setTotalWithdrawn(nz(w.getTotalWithdrawn()).add(r.getAmount()));
        w.setUpdateAt(LocalDateTime.now());
        walletRepository.save(w);

        log.info("[Withdraw] #{} APPROVED by admin, amount {}", requestId, r.getAmount());
        return toWithdrawResponse(r);
    }

    @Override
    @Transactional
    public com.sang.be_capnong.dto.response.wallet.WithdrawResponse rejectWithdraw(int requestId, String note) {
        WithdrawRequest r = lockWithdrawPending(requestId);
        finishWithdraw(r, WithdrawStatus.REJECTED, note);

        // Trả lại số dư cho ví (bút toán dương)
        credit(r.getWallet(), null, WalletTransactionType.WITHDRAW_REFUNDED, r.getAmount(),
                "Hoàn lại số dư lệnh rút #" + r.getRequestId() + (note != null ? " (" + note.trim() + ")" : ""));
        log.info("[Withdraw] #{} REJECTED by admin, amount {} refunded to wallet", requestId, r.getAmount());
        return toWithdrawResponse(r);
    }

    private WithdrawRequest lockWithdrawPending(int requestId) {
        WithdrawRequest r = withdrawRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.WITHDRAW_NOT_FOUND));
        if (r.getStatus() != WithdrawStatus.PENDING) {
            throw new AppException(ErrorCode.WITHDRAW_ALREADY_PROCESSED);
        }
        return r;
    }

    private void finishWithdraw(WithdrawRequest r, WithdrawStatus status, String note) {
        r.setStatus(status);
        r.setAdminNote(trimToNull(note));
        r.setAdmin(currentUser());
        r.setProcessedAt(LocalDateTime.now());
        withdrawRequestRepository.save(r);
    }

    private com.sang.be_capnong.dto.response.wallet.WithdrawResponse toWithdrawResponse(WithdrawRequest r) {
        Wallet w = r.getWallet();
        User owner = w.getUser();
        return com.sang.be_capnong.dto.response.wallet.WithdrawResponse.builder()
                .id(r.getRequestId())
                .walletId(w.getWalletId())
                .walletType(w.getType())
                .userId(owner.getId())
                .ownerName(owner.getShopName() != null ? owner.getShopName() : owner.getFullName())
                .ownerEmail(owner.getEmail())
                .amount(r.getAmount())
                .fee(r.getFee())
                .receiveAmount(r.getReceiveAmount())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .bankName(r.getBankName())
                .bankAccountNumber(r.getBankAccountNumber())
                .bankAccountHolder(r.getBankAccountHolder())
                .note(r.getReason())
                .adminNote(r.getAdminNote())
                .adminName(r.getAdmin() != null ? r.getAdmin().getFullName() : null)
                .createdAt(r.getCreatedAt())
                .processedAt(r.getProcessedAt())
                .build();
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String maskAccount(String number) {
        return number.length() <= 4 ? number : "***" + number.substring(number.length() - 4);
    }

    // ───────────────────────────── Tổng hợp ví sàn ─────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse getPlatformOverview(String period) {
        String p = period == null ? "30D" : period.trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = switch (p) {
            case "TODAY" -> now.toLocalDate().atStartOfDay();
            case "7D" -> now.minusDays(7);
            case "ALL" -> LocalDateTime.of(2000, 1, 1, 0, 0);
            default -> { p = "30D"; yield now.minusDays(30); }
        };

        Wallet platform = getPlatformWalletEntity();

        // Hoa hồng và tiền sàn bù trong kỳ (bút toán ví sàn)
        Map<WalletTransactionType, BigDecimal> sums = new EnumMap<>(WalletTransactionType.class);
        for (Object[] r : walletTransactionRepository.sumByTypeSince(platform.getWalletId(), from)) {
            sums.put((WalletTransactionType) r[0], nz((BigDecimal) r[1]));
        }
        BigDecimal farmerFee = sums.getOrDefault(WalletTransactionType.PLATFORM_FARMER_FEE, BigDecimal.ZERO);
        BigDecimal shipperFee = sums.getOrDefault(WalletTransactionType.PLATFORM_SHIPPER_FEE, BigDecimal.ZERO);

        // Ví Shop / Shipper
        List<Wallet> partnerWallets = walletRepository.findByTypeIn(List.of(TYPE_SHOP, TYPE_SHIPPER));
        Map<Integer, Integer> pendingByWallet = new HashMap<>();
        Set<Integer> heldOrders = new HashSet<>();
        Set<Integer> shopHeldOrders = new HashSet<>();
        Set<Integer> shipperHeldOrders = new HashSet<>();
        Map<Integer, String> walletType = new HashMap<>();
        partnerWallets.forEach(w -> walletType.put(w.getWalletId(), w.getType()));
        for (Object[] r : walletTransactionRepository.openHoldsByWalletAndOrder(HOLD_TYPES)) {
            int wid = (Integer) r[0];
            int oid = (Integer) r[1];
            String t = walletType.get(wid);
            if (t == null) continue; // ví sàn
            pendingByWallet.merge(wid, 1, Integer::sum);
            heldOrders.add(oid);
            (TYPE_SHOP.equals(t) ? shopHeldOrders : shipperHeldOrders).add(oid);
        }

        // COD chưa nộp
        List<Order> codPending = orderRepository.findByCodCashConfirmedAtIsNotNullAndCodSettledFalseOrderByCodCashConfirmedAtAsc();
        Map<Integer, BigDecimal> codByShipper = new HashMap<>();
        for (Order o : codPending) {
            if (o.getShipper() != null) codByShipper.merge(o.getShipper().getId(), nz(o.getCodAmount()), BigDecimal::add);
        }

        BigDecimal shopHeld = BigDecimal.ZERO, shipperHeld = BigDecimal.ZERO;
        BigDecimal shopAvailable = BigDecimal.ZERO, shipperAvailable = BigDecimal.ZERO;
        List<com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse.Partner> partners = new ArrayList<>();
        for (Wallet w : partnerWallets) {
            boolean shop = TYPE_SHOP.equals(w.getType());
            BigDecimal held = nz(w.getFrozenBalance());
            BigDecimal avail = nz(w.getTotalBalance());
            if (shop) {
                shopHeld = shopHeld.add(held);
                shopAvailable = shopAvailable.add(avail);
            } else {
                shipperHeld = shipperHeld.add(held);
                shipperAvailable = shipperAvailable.add(avail);
            }
            User u = w.getUser();
            int pending = pendingByWallet.getOrDefault(w.getWalletId(), 0);
            BigDecimal codDebt = shop ? null : codByShipper.getOrDefault(u.getId(), BigDecimal.ZERO);
            partners.add(com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse.Partner.builder()
                    .userId(u.getId())
                    .walletId(w.getWalletId())
                    .name(u.getShopName() != null ? u.getShopName() : u.getFullName())
                    .role(w.getType())
                    .balance(avail)
                    .held(held)
                    .pendingOrders(pending)
                    .codHolding(codDebt)
                    .build());
        }

        // Tổng hợp đơn hoàn trả & đã quyết toán
        List<Order> refunds = orderRepository.findByRefundAmountGreaterThanOrderByCreatedAtDesc(BigDecimal.ZERO);
        List<Order> settled = orderRepository.findBySettledAtGreaterThanEqual(from);

        return com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse.builder()
                .period(p)
                .from(from)
                .shopHeld(shopHeld)
                .shipperHeld(shipperHeld)
                .shopHeldOrders(shopHeldOrders.size())
                .shipperHeldOrders(shipperHeldOrders.size())
                .heldOrders(heldOrders.size())
                .farmerFee(farmerFee)
                .shipperFee(shipperFee)
                .commissionTotal(farmerFee.add(shipperFee))
                .platformBalance(nz(platform.getTotalBalance()))
                .platformHeld(nz(platform.getFrozenBalance()))
                .voucherSubsidy(sums.getOrDefault(WalletTransactionType.PLATFORM_VOUCHER_SUBSIDY, BigDecimal.ZERO).negate())
                .freeshipSubsidy(sums.getOrDefault(WalletTransactionType.PLATFORM_FREESHIP_SUBSIDY, BigDecimal.ZERO).negate())
                .codPendingAmount(codPending.stream().map(o -> nz(o.getCodAmount())).reduce(BigDecimal.ZERO, BigDecimal::add))
                .codPendingOrders(codPending.size())
                .codPendingShippers(codByShipper.size())
                .codOldestCollectedAt(codPending.isEmpty() ? null : codPending.get(0).getCodCashConfirmedAt())
                .refundAmount(refunds.stream().map(o -> nz(o.getRefundAmount())).reduce(BigDecimal.ZERO, BigDecimal::add))
                .refundOrders(refunds.size())
                .shopAvailable(shopAvailable)
                .shipperAvailable(shipperAvailable)
                .settledGmv(settled.stream().map(o -> nz(o.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add))
                .settledOrders(settled.size())
                .partners(partners)
                .build();
    }

    // ───────────────────────────── Xem ví ─────────────────────────────

    @Override
    @Transactional
    public WalletResponse getMyWallet() {
        User me = currentUser();
        if (hasRole(me, "ADMIN")) {
            return toResponse(getPlatformWalletEntity());
        }
        String type = hasRole(me, "SHIPPER") ? TYPE_SHIPPER
                : hasRole(me, "SHOP") ? TYPE_SHOP
                : (hasRole(me, "BUYER") || hasRole(me, "USER")) ? TYPE_BUYER
                : null;
        if (type == null) {
            throw new AppException(ErrorCode.WALLET_NOT_FOUND);
        }
        return toResponse(getOrCreateWallet(me, type));
    }

    @Override
    @Transactional
    public Page<WalletTransactionResponse> getMyTransactions(int page, int size) {
        WalletResponse w = getMyWallet();
        return walletTransactionRepository
                .findByWallet_WalletIdOrderByCreatedAtDescIdDesc(w.getWalletId(), PageRequest.of(Math.max(page, 0), clampSize(size)))
                .map(this::toTxResponse);
    }

    @Override
    @Transactional
    public WalletResponse getPlatformWallet() {
        return toResponse(getPlatformWalletEntity());
    }

    @Override
    @Transactional
    public Page<WalletTransactionResponse> getPlatformTransactions(int page, int size) {
        Wallet w = getPlatformWalletEntity();
        return walletTransactionRepository
                .findByWallet_WalletIdOrderByCreatedAtDescIdDesc(w.getWalletId(), PageRequest.of(Math.max(page, 0), clampSize(size)))
                .map(this::toTxResponse);
    }

    // ───────────────────────────── Tính toán ─────────────────────────────

    /** Kết quả chia tiền của 1 đơn, làm tròn tới đồng */
    private record Split(BigDecimal goods, BigDecimal shippingFee, BigDecimal shopVoucher,
                         BigDecimal platformVoucher, BigDecimal platformFreeship, BigDecimal buyerPaid,
                         BigDecimal farmerFee, BigDecimal shipperFee, BigDecimal shopReceive,
                         BigDecimal shipperReceive, BigDecimal platformReceive, BigDecimal platformVoucherSubsidy) {
    }

    private Split computeSplit(Order order) {
        return computeSplit(order, false);
    }

    /** assumeShipper = true: tính như đơn sẽ có Shipper giao (dùng khi tạm giữ, lúc chưa có Shipper nhận) */
    private Split computeSplit(Order order, boolean assumeShipper) {
        boolean withShipper = assumeShipper || order.getShipper() != null;
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
        BigDecimal goods = details.stream()
                .map(d -> nz(d.getUnitPrice()).multiply(BigDecimal.valueOf(d.getQuantity() != null ? d.getQuantity() : 0)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = nz(order.getShippingFee());

        BigDecimal refundAmount = nz(order.getRefundAmount());
        BigDecimal netGoods = goods.subtract(refundAmount).max(BigDecimal.ZERO);

        BigDecimal shopVoucher = BigDecimal.ZERO;
        BigDecimal platformVoucher = BigDecimal.ZERO;
        BigDecimal platformFreeship = BigDecimal.ZERO;
        List<VoucherUsage> usages = voucherUsageRepository.findByOrder(order);
        if (usages != null) {
            for (VoucherUsage u : usages) {
                BigDecimal amt = nz(u.getDiscountAmount());
                Voucher v = u.getVoucher();
                boolean adminVoucher = v != null && (
                        (v.getShop() != null && v.getShop().getRole() != null && "ADMIN".equalsIgnoreCase(v.getShop().getRole().getName()))
                        || v.getVoucherScope() == com.sang.be_capnong.enums.VoucherScope.PLATFORM
                        || v.getVoucherScope() == com.sang.be_capnong.enums.VoucherScope.SYSTEM
                        || v.getVoucherScope() == com.sang.be_capnong.enums.VoucherScope.WELCOME
                );
                if (!adminVoucher) {
                    shopVoucher = shopVoucher.add(amt);
                } else if (isShippingVoucher(v)) {
                    platformFreeship = platformFreeship.add(amt);
                } else {
                    platformVoucher = platformVoucher.add(amt);
                }
            }
        }

        goods = money(goods);
        netGoods = money(netGoods);
        shippingFee = money(shippingFee);
        shopVoucher = money(shopVoucher).min(netGoods.add(shippingFee));
        platformVoucher = money(platformVoucher);
        platformFreeship = money(platformFreeship).min(shippingFee);

        BigDecimal buyerPaid = money(nz(order.getTotalAmount()));
        BigDecimal feeBase = netGoods.subtract(shopVoucher).max(BigDecimal.ZERO);
        BigDecimal farmerFee = money(feeBase.multiply(farmerFeeRate));
        BigDecimal shopReceive = feeBase.subtract(farmerFee);
        BigDecimal shipperFee = withShipper ? money(shippingFee.multiply(shipperFeeRate)) : BigDecimal.ZERO;
        BigDecimal shipperReceive = withShipper ? shippingFee.subtract(shipperFee) : BigDecimal.ZERO;

        // Phần còn lại thuộc về sàn. Bất biến: buyerPaid = shopReceive + shipperReceive + platformReceive + refundAmount
        BigDecimal platformReceive = buyerPaid.subtract(shopReceive).subtract(shipperReceive).subtract(refundAmount);
        BigDecimal platformVoucherSubsidy = farmerFee.add(shipperFee).subtract(platformFreeship).subtract(platformReceive);

        return new Split(goods, shippingFee, shopVoucher, platformVoucher, platformFreeship, buyerPaid,
                farmerFee, shipperFee, shopReceive, shipperReceive, platformReceive, platformVoucherSubsidy);
    }

    private boolean isShippingVoucher(Voucher v) {
        if (v == null) return false;
        if (v.getVoucherTarget() == VoucherTarget.SHIPPING) return true;
        if (v.getVoucherType() == VoucherType.FREE_SHIPPING) return true;
        return v.getVoucherTarget() == null && v.getVoucherCode() != null
                && v.getVoucherCode().toUpperCase().contains("FREESHIP");
    }

    private OrderSettlementResponse buildSettlement(Order order) {
        Split s = computeSplit(order);
        List<WalletTransactionResponse> txs = walletTransactionRepository.findByOrder_OrderIdOrderByIdAsc(order.getOrderId())
                .stream().map(this::toTxResponse).toList();
        return OrderSettlementResponse.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentMethod(order.getPaymentMethod())
                .settled(order.getSettledAt() != null)
                .settledAt(order.getSettledAt())
                .goodsAmount(s.goods)
                .shippingFee(s.shippingFee)
                .shopVoucherDiscount(s.shopVoucher)
                .platformVoucherDiscount(s.platformVoucher)
                .platformFreeshipDiscount(s.platformFreeship)
                .buyerPaid(s.buyerPaid)
                .farmerFeeRate(farmerFeeRate)
                .farmerFee(s.farmerFee)
                .shipperFeeRate(shipperFeeRate)
                .shipperFee(s.shipperFee)
                .shop(party(order.getShop()))
                .shopReceive(s.shopReceive)
                .shipper(party(order.getShipper()))
                .shipperReceive(s.shipperReceive)
                .platformReceive(s.platformReceive)
                .codAmount(order.getCodAmount())
                .codCashCollectedAt(order.getCodCashConfirmedAt())
                .codSettled(order.getCodSettled())
                .codSettledAt(order.getCodSettledAt())
                .refundAmount(order.getRefundAmount())
                .refundReason(order.getRefundReason())
                .transactions(txs)
                .build();
    }

    // ───────────────────────────── Ví & bút toán ─────────────────────────────

    private Wallet getOrCreateWallet(User user, String type) {
        if (TYPE_BUYER.equals(type)) {
            return walletRepository.findFirstByUser_IdAndType(user.getId(), TYPE_BUYER)
                    .or(() -> walletRepository.findFirstByUser_IdAndType(user.getId(), TYPE_REFERRAL))
                    .orElseGet(() -> walletRepository.save(Wallet.builder()
                            .user(user)
                            .type(TYPE_BUYER)
                            .status(WalletStatus.ACTIVE)
                            .createAt(LocalDateTime.now())
                            .updateAt(LocalDateTime.now())
                            .build()));
        }
        return walletRepository.findFirstByUser_IdAndType(user.getId(), type)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .user(user)
                        .type(type)
                        .status(WalletStatus.ACTIVE)
                        .createAt(LocalDateTime.now())
                        .updateAt(LocalDateTime.now())
                        .build()));
    }

    private Wallet getPlatformWalletEntity() {
        return walletRepository.findFirstByType(TYPE_PLATFORM).orElseGet(() -> {
            User admin = userRepository.findByRole_NameIn(List.of("ADMIN")).stream()
                    .min(Comparator.comparingInt(User::getId))
                    .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_OWNER_MISSING));
            return getOrCreateWallet(admin, TYPE_PLATFORM);
        });
    }

    /** Cộng (amount > 0) hoặc trừ (amount < 0) ví và ghi sổ cái. Bỏ qua bút toán 0đ. */
    private void credit(Wallet wallet, Order order, WalletTransactionType type, BigDecimal amount, String description) {
        if (amount == null || amount.signum() == 0) {
            return;
        }
        Wallet locked = walletRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        BigDecimal newBalance = nz(locked.getTotalBalance()).add(amount);
        locked.setTotalBalance(newBalance);
        if (type == WalletTransactionType.SHOP_INCOME || type == WalletTransactionType.SHIPPER_INCOME
                || type == WalletTransactionType.SHOP_REFUND_DEDUCT) {
            locked.setTotalRevenueAllTime(nz(locked.getTotalRevenueAllTime()).add(amount).max(BigDecimal.ZERO));
        }
        if (type == WalletTransactionType.PLATFORM_FARMER_FEE || type == WalletTransactionType.PLATFORM_SHIPPER_FEE
                || type == WalletTransactionType.PLATFORM_REFUND_DEDUCT) {
            locked.setCommissionWallet(nz(locked.getCommissionWallet()).add(amount).max(BigDecimal.ZERO));
            locked.setTotalRevenueAllTime(nz(locked.getTotalRevenueAllTime()).add(amount).max(BigDecimal.ZERO));
        }
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .order(order)
                .type(type)
                .amount(amount)
                .balanceAfter(newBalance)
                .frozenAfter(nz(locked.getFrozenBalance()))
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void creditReferralCommission(User referrer, Order order, Long referralCommissionId, BigDecimal amount, String description) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        Wallet buyerWallet = getOrCreateWallet(referrer, TYPE_BUYER);
        Wallet locked = walletRepository.findByIdForUpdate(buyerWallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        BigDecimal newBalance = money(nz(locked.getTotalBalance()).add(amount));
        locked.setTotalBalance(newBalance);
        locked.setTotalRevenueAllTime(money(nz(locked.getTotalRevenueAllTime()).add(amount)));
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .order(order)
                .referralCommissionId(referralCommissionId)
                .type(WalletTransactionType.REFERRAL_COMMISSION)
                .amount(amount)
                .balanceAfter(newBalance)
                .frozenAfter(nz(locked.getFrozenBalance()))
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("[Wallet] Credited referral commission {} to referrer #{} (buyer wallet #{}). New balance={}",
                amount, referrer.getId(), locked.getWalletId(), newBalance);
    }

    private WalletResponse toResponse(Wallet w) {
        BigDecimal codHolding = null;
        if (TYPE_SHIPPER.equals(w.getType())) {
            codHolding = orderRepository.findByCodCashConfirmedAtIsNotNullAndCodSettledFalseOrderByCodCashConfirmedAtAsc().stream()
                    .filter(o -> o.getShipper() != null && o.getShipper().getId() == w.getUser().getId())
                    .map(o -> nz(o.getCodAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        User owner = w.getUser();
        return WalletResponse.builder()
                .walletId(w.getWalletId())
                .type(w.getType())
                .userId(owner.getId())
                .ownerName(TYPE_PLATFORM.equals(w.getType()) ? "Sàn CapNong"
                        : owner.getShopName() != null ? owner.getShopName() : owner.getFullName())
                .status(w.getStatus() != null ? w.getStatus().name() : null)
                .balance(nz(w.getTotalBalance()))
                .frozenBalance(nz(w.getFrozenBalance()))
                .totalRevenueAllTime(nz(w.getTotalRevenueAllTime()))
                .totalWithdrawn(nz(w.getTotalWithdrawn()))
                .commissionEarned(TYPE_PLATFORM.equals(w.getType()) ? nz(w.getCommissionWallet()) : null)
                .codCashHolding(codHolding)
                .codReserved(nz(w.getCodReserved()))
                .availableBalance(nz(w.getTotalBalance()).subtract(nz(w.getCodReserved())))
                .updatedAt(w.getUpdateAt())
                .build();
    }

    private WalletTransactionResponse toTxResponse(WalletTransaction t) {
        return WalletTransactionResponse.builder()
                .id(t.getId())
                .walletId(t.getWallet().getWalletId())
                .walletType(t.getWallet().getType())
                .orderId(t.getOrder() != null ? t.getOrder().getOrderId() : null)
                .type(t.getType().name())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .frozenAfter(t.getFrozenAfter())
                .hold(HOLD_TYPES.contains(t.getType()))
                .reserve(t.getType() == WalletTransactionType.COD_RESERVED
                        || t.getType() == WalletTransactionType.COD_RESERVE_RELEASED)
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }

    // ───────────────────────────── Tiện ích ─────────────────────────────

    private User currentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(Integer.parseInt(subject))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private static boolean hasRole(User u, String role) {
        return u.getRole() != null && role.equalsIgnoreCase(u.getRole().getName());
    }

    private static OrderSettlementResponse.Party party(User u) {
        if (u == null) return null;
        return OrderSettlementResponse.Party.builder()
                .userId(u.getId())
                .name(u.getShopName() != null ? u.getShopName() : u.getFullName())
                .build();
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal money(BigDecimal v) {
        return nz(v).setScale(0, RoundingMode.HALF_UP);
    }

    private static String vnd(BigDecimal v) {
        return String.format(Locale.US, "%,.0fđ", nz(v)).replace(',', '.');
    }

    private static String pct(BigDecimal rate) {
        return nz(rate).multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}
