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
    /** Ví giới thiệu của Buyer (đặc tả: Social Commerce & Referral Wallet) */
    public static final String TYPE_REFERRAL = "REFERRAL";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserRepository userRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;

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
        if (nz(locked.getTotalBalance()).compareTo(amount) < 0) {
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
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            list = withdrawRequestRepository.findAllByOrderByCreatedAtDesc();
        } else {
            WithdrawStatus st;
            try {
                st = WithdrawStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_STATUS);
            }
            list = withdrawRequestRepository.findByStatusOrderByCreatedAtAsc(st);
        }
        return list.stream().map(this::toWithdrawResponse).toList();
    }

    @Override
    @Transactional
    public com.sang.be_capnong.dto.response.wallet.WithdrawResponse approveWithdraw(int requestId, String note) {
        WithdrawRequest r = lockPendingWithdraw(requestId);
        Wallet w = walletRepository.findByIdForUpdate(r.getWallet().getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        w.setTotalWithdrawn(nz(w.getTotalWithdrawn()).add(nz(r.getAmount())));
        w.setUpdateAt(LocalDateTime.now());
        walletRepository.save(w);
        finishWithdraw(r, WithdrawStatus.COMPLETED, note);
        log.info("[Withdraw] #{} COMPLETED", requestId);
        return toWithdrawResponse(r);
    }

    @Override
    @Transactional
    public com.sang.be_capnong.dto.response.wallet.WithdrawResponse rejectWithdraw(int requestId, String note) {
        WithdrawRequest r = lockPendingWithdraw(requestId);
        credit(r.getWallet(), null, WalletTransactionType.WITHDRAW_REFUNDED, nz(r.getAmount()),
                "Lệnh rút #" + requestId + " bị từ chối, hoàn " + vnd(r.getAmount()) + " vào ví"
                        + (trimToNull(note) != null ? ": " + note.trim() : ""));
        finishWithdraw(r, WithdrawStatus.REJECTED, note);
        log.info("[Withdraw] #{} REJECTED", requestId);
        return toWithdrawResponse(r);
    }

    private WithdrawRequest lockPendingWithdraw(int requestId) {
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
            BigDecimal bal = nz(w.getTotalBalance());
            if (shop) { shopHeld = shopHeld.add(held); shopAvailable = shopAvailable.add(bal); }
            else { shipperHeld = shipperHeld.add(held); shipperAvailable = shipperAvailable.add(bal); }
            User u = w.getUser();
            partners.add(com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse.Partner.builder()
                    .userId(u.getId())
                    .walletId(w.getWalletId())
                    .name(u.getShopName() != null ? u.getShopName() : u.getFullName())
                    .role(w.getType())
                    .held(held)
                    .balance(bal)
                    .pendingOrders(pendingByWallet.getOrDefault(w.getWalletId(), 0))
                    .codHolding(shop ? null : codByShipper.getOrDefault(u.getId(), BigDecimal.ZERO))
                    .build());
        }
        partners.sort(Comparator.comparing(com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse.Partner::getHeld).reversed()
                .thenComparing(com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse.Partner::getName,
                        Comparator.nullsLast(Comparator.naturalOrder())));

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
                : hasRole(me, "BUYER") ? TYPE_REFERRAL
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
