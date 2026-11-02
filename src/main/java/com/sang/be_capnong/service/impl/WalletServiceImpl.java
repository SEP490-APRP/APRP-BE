package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.response.wallet.OrderSettlementResponse;
import com.sang.be_capnong.dto.response.wallet.RefundDueResponse;
import com.sang.be_capnong.dto.response.wallet.WalletResponse;
import com.sang.be_capnong.dto.response.wallet.WalletTransactionResponse;
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

/**
 * Quyết toán ví theo đặc tả CapNong:
 * - Phí dịch vụ nông dân: {farmerFeeRate} x (tiền hàng - voucher shop), trừ vào tiền Shop nhận.
 * - Phí dịch vụ shipper: {shipperFeeRate} x cước giao hàng, trừ vào tiền Shipper nhận.
 * - Voucher shop: Shop chịu. Voucher sàn (giảm tiền hàng / freeship): sàn bù, Shop và Shipper nhận đủ.
 * Chia tiền khi đơn COMPLETED. Bất biến: buyerPaid = shopReceive + shipperReceive + platformReceive.
 *
 * Tiền tạm giữ (frozenBalance): Shop + Sàn thấy phần của mình ngay khi tiền vào sàn
 * (PayOS PAID, COD lúc Shipper thu tiền mặt); Shipper thấy cước ngay khi nhận đơn.
 * Khi COMPLETED tiền tạm giữ được gỡ và cộng vào số dư rút được; huỷ / thất bại thì gỡ bỏ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    public static final String TYPE_SHOP = "SHOP";
    public static final String TYPE_SHIPPER = "SHIPPER";
    public static final String TYPE_PLATFORM = "PLATFORM";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserRepository userRepository;

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
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.ORDER_NOT_SETTLEABLE);
        }

        Split s = computeSplit(order);
        String ref = "Đơn #" + orderId;

        releaseHolds(order, WalletTransactionType.HOLD_RELEASED, ref + ": đơn hoàn tất, chuyển tiền tạm giữ sang số dư");

        Wallet shopWallet = getOrCreateWallet(order.getShop(), TYPE_SHOP);
        credit(shopWallet, order, WalletTransactionType.SHOP_INCOME, s.shopReceive,
                ref + ": tiền hàng " + vnd(s.goods) + " - voucher shop " + vnd(s.shopVoucher)
                        + " - phí dịch vụ " + pct(farmerFeeRate) + " " + vnd(s.farmerFee));

        if (order.getShipper() != null && s.shipperReceive.signum() != 0) {
            Wallet shipperWallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
            credit(shipperWallet, order, WalletTransactionType.SHIPPER_INCOME, s.shipperReceive,
                    ref + ": cước giao " + vnd(s.shippingFee) + " - phí dịch vụ " + pct(shipperFeeRate) + " " + vnd(s.shipperFee));
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
        if (order.getShipper() != null) {
            order.setShipperPaidAt(order.getSettledAt());
        }
        orderRepository.save(order);

        log.info("[Wallet] Settled order #{}: buyerPaid={}, shop={}, shipper={}, platform={}",
                orderId, s.buyerPaid, s.shopReceive, s.shipperReceive, s.platformReceive);
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

    private static final Set<WalletTransactionType> HOLD_TYPES = EnumSet.of(
            WalletTransactionType.SHOP_INCOME_HOLD, WalletTransactionType.SHIPPER_INCOME_HOLD,
            WalletTransactionType.PLATFORM_INCOME_HOLD, WalletTransactionType.HOLD_RELEASED,
            WalletTransactionType.HOLD_CANCELLED);

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
        BigDecimal newFrozen = nz(locked.getFrozenBalance()).add(amount);
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
        order.setCodAmount(cash);
        order.setCodCashConfirmedAt(LocalDateTime.now());
        order.setCodSettled(false);
        order.setCodPaymentStatus(CodPaymentStatus.CONFIRMED);
        orderRepository.save(order);

        Wallet shipperWallet = getOrCreateWallet(order.getShipper(), TYPE_SHIPPER);
        credit(shipperWallet, order, WalletTransactionType.COD_CASH_COLLECTED, cash.negate(),
                "Đơn #" + orderId + ": giữ tiền mặt COD " + vnd(cash) + " của khách, cần nộp về sàn");
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

    // ───────────────────────────── Xem ví ─────────────────────────────

    @Override
    @Transactional
    public WalletResponse getMyWallet() {
        User me = currentUser();
        if (hasRole(me, "ADMIN")) {
            return toResponse(getPlatformWalletEntity());
        }
        String type = hasRole(me, "SHIPPER") ? TYPE_SHIPPER : hasRole(me, "SHOP") ? TYPE_SHOP : null;
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

        BigDecimal shopVoucher = BigDecimal.ZERO;
        BigDecimal platformVoucher = BigDecimal.ZERO;
        BigDecimal platformFreeship = BigDecimal.ZERO;
        List<VoucherUsage> usages = voucherUsageRepository.findByOrder(order);
        if (usages != null) {
            for (VoucherUsage u : usages) {
                BigDecimal amt = nz(u.getDiscountAmount());
                Voucher v = u.getVoucher();
                boolean adminVoucher = v != null && v.getShop() != null && v.getShop().getRole() != null
                        && "ADMIN".equalsIgnoreCase(v.getShop().getRole().getName());
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
        shippingFee = money(shippingFee);
        shopVoucher = money(shopVoucher).min(goods.add(shippingFee));
        platformVoucher = money(platformVoucher);
        platformFreeship = money(platformFreeship).min(shippingFee);

        BigDecimal buyerPaid = money(nz(order.getTotalAmount()));
        BigDecimal feeBase = goods.subtract(shopVoucher).max(BigDecimal.ZERO);
        BigDecimal farmerFee = money(feeBase.multiply(farmerFeeRate));
        BigDecimal shopReceive = feeBase.subtract(farmerFee);
        BigDecimal shipperFee = withShipper ? money(shippingFee.multiply(shipperFeeRate)) : BigDecimal.ZERO;
        BigDecimal shipperReceive = withShipper ? shippingFee.subtract(shipperFee) : BigDecimal.ZERO;

        // Phần còn lại thuộc về sàn. Tính theo phần dư để sổ cái luôn cân (kể cả khi làm tròn).
        BigDecimal platformReceive = buyerPaid.subtract(shopReceive).subtract(shipperReceive);
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
        if (type == WalletTransactionType.SHOP_INCOME || type == WalletTransactionType.SHIPPER_INCOME) {
            locked.setTotalRevenueAllTime(nz(locked.getTotalRevenueAllTime()).add(amount));
        }
        if (type == WalletTransactionType.PLATFORM_FARMER_FEE || type == WalletTransactionType.PLATFORM_SHIPPER_FEE) {
            locked.setCommissionWallet(nz(locked.getCommissionWallet()).add(amount));
            locked.setTotalRevenueAllTime(nz(locked.getTotalRevenueAllTime()).add(amount));
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
