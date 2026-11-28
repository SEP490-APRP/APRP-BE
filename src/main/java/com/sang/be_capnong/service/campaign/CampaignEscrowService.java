package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.WalletStatus;
import com.sang.be_capnong.enums.WalletTransactionType;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.repository.WalletRepository;
import com.sang.be_capnong.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Bút toán ví cho chiến dịch giải cứu (escrow tiền cọc).
 * Tiền cọc nằm trong frozenBalance của ví Sàn tới khi giải ngân cho nhà vườn hoặc hoàn cho người mua.
 * Tính idempotent do caller đảm bảo: chỉ gọi khi đã khoá dòng CampaignDeposit / PreOrder và kiểm tra trạng thái.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignEscrowService {

    static final String TYPE_PLATFORM = "PLATFORM";
    static final String TYPE_SHOP = "SHOP";
    static final String TYPE_BUYER = "BUYER";
    static final String TYPE_REFERRAL = "REFERRAL";
    static final String TYPE_SHIPPER = "SHIPPER";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    /** Số dư ví người mua dùng được để trả cọc */
    @Transactional(readOnly = true)
    public BigDecimal buyerBalance(User buyer) {
        Wallet w = userWallet(buyer, TYPE_BUYER);
        return nz(w.getTotalBalance()).subtract(nz(w.getFrozenBalance())).max(BigDecimal.ZERO);
    }

    /** Người mua trả (một phần) cọc bằng ví: trừ số dư ví người mua */
    @Transactional(propagation = Propagation.MANDATORY)
    public void payDepositFromWallet(PreOrder p, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        Wallet w = walletRepository.findByIdForUpdate(userWallet(p.getBuyer(), TYPE_BUYER).getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        if (nz(w.getTotalBalance()).subtract(nz(w.getFrozenBalance())).compareTo(amount) < 0) {
            throw new AppException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }
        adjust(w, BigDecimal.ZERO, amount.negate(), WalletTransactionType.CAMPAIGN_DEPOSIT_WALLET_PAID,
                "Trả cọc đơn đặt trước " + p.getCode() + " bằng ví (chiến dịch " + p.getCampaign().getCode() + ")");
    }

    /** Đơn đặt trước huỷ khi chưa cọc xong: trả lại phần đã trừ ví */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundDepositWalletPart(PreOrder p, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        adjust(userWallet(p.getBuyer(), TYPE_BUYER), BigDecimal.ZERO, amount, WalletTransactionType.CAMPAIGN_DEPOSIT_WALLET_REFUND,
                "Hoàn phần cọc đã trả bằng ví, đơn đặt trước " + p.getCode() + " chưa cọc xong đã huỷ");
    }

    /** Cọc PayOS đã vào sàn: ví Sàn tạm giữ (frozen +) */
    @Transactional(propagation = Propagation.MANDATORY)
    public void holdDeposit(CampaignDeposit deposit) {
        PreOrder p = deposit.getPreOrder();
        adjust(platformWallet(), deposit.getAmount(), BigDecimal.ZERO, WalletTransactionType.CAMPAIGN_DEPOSIT_HOLD,
                "Tạm giữ tiền cọc đơn đặt trước " + p.getCode() + " (chiến dịch " + p.getCampaign().getCode() + ")");
    }

    /** Chiến dịch huỷ: Sàn gỡ tạm giữ, người mua nhận lại cọc vào ví CapNong */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundDeposit(CampaignDeposit deposit) {
        PreOrder p = deposit.getPreOrder();
        BigDecimal amt = deposit.getAmount();
        adjust(platformWallet(), amt.negate(), BigDecimal.ZERO, WalletTransactionType.CAMPAIGN_DEPOSIT_REFUNDED,
                "Hoàn cọc đơn " + p.getCode() + " do chiến dịch " + p.getCampaign().getCode() + " bị huỷ");
        adjust(userWallet(p.getBuyer(), TYPE_BUYER), BigDecimal.ZERO, amt, WalletTransactionType.CAMPAIGN_DEPOSIT_REFUNDED,
                "Nhận lại tiền cọc đơn đặt trước " + p.getCode() + " (chiến dịch " + p.getCampaign().getCode() + " bị huỷ)");
    }

    /** Thiếu hàng: hoàn một phần cọc (thùng không giao được) vào ví người mua */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundDepositPart(CampaignDeposit deposit, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        PreOrder p = deposit.getPreOrder();
        adjust(platformWallet(), amount.negate(), BigDecimal.ZERO, WalletTransactionType.CAMPAIGN_DEPOSIT_REFUNDED,
                "Hoàn cọc phần thiếu hàng đơn " + p.getCode() + " (chiến dịch " + p.getCampaign().getCode() + ")");
        adjust(userWallet(p.getBuyer(), TYPE_BUYER), BigDecimal.ZERO, amount, WalletTransactionType.CAMPAIGN_DEPOSIT_REFUNDED,
                "Nhận lại tiền cọc do thiếu hàng, đơn " + p.getCode() + " (chiến dịch " + p.getCampaign().getCode() + ")");
    }

    /** Giải ngân cọc: Sàn gỡ tạm giữ, tiền chuyển sang số dư Sàn để chi trả cho nhà vườn */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseDeposit(CampaignDeposit deposit) {
        PreOrder p = deposit.getPreOrder();
        BigDecimal amt = deposit.getAmount();
        adjust(platformWallet(), amt.negate(), amt, WalletTransactionType.CAMPAIGN_DEPOSIT_RELEASED,
                "Giải ngân tiền cọc đơn " + p.getCode() + " cho nhà vườn chiến dịch " + p.getCampaign().getCode());
    }

    /** Shipper thu phần còn lại khi giao: ghi nhận vào số dư Sàn */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordRemainingCollected(PreOrder p) {
        adjust(platformWallet(), BigDecimal.ZERO, p.getRemainingAmount(), WalletTransactionType.CAMPAIGN_REMAINING_COLLECTED,
                "Thu phần còn lại đơn đặt trước " + p.getCode() + " khi giao hàng");
    }

    /** Chi trả cho nhà vườn: Sàn trừ số dư, ví nhà vườn cộng */
    @Transactional(propagation = Propagation.MANDATORY)
    public void payoutFarm(CampaignFarmRegistration reg, BigDecimal netAmount) {
        if (netAmount == null || netAmount.signum() <= 0) return;
        String c = reg.getCampaign().getCode();
        adjust(platformWallet(), BigDecimal.ZERO, netAmount.negate(), WalletTransactionType.CAMPAIGN_FARM_PAYOUT,
                "Chi trả nhà vườn #" + reg.getShop().getId() + " chiến dịch " + c);
        Wallet shop = userWallet(reg.getShop(), TYPE_SHOP);
        adjust(shop, BigDecimal.ZERO, netAmount, WalletTransactionType.CAMPAIGN_FARM_INCOME,
                "Tiền hàng chiến dịch " + c + " (" + reg.getReceivedWeightKg() + " kg, đã trừ phí dịch vụ)");
    }

    // ───────────── Shipper: ký quỹ tiền thu hộ + cước giao ─────────────

    /**
     * Shipper lấy đơn khỏi hub: giữ chỗ trong ví đúng số tiền sẽ thu của người mua (giống ký quỹ COD đơn thường).
     * Ví đang âm (còn nợ) hoặc số dư khả dụng không đủ thì không cho nhận.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal reserveShipperCod(PreOrder p, User shipper) {
        Wallet w = walletRepository.findByIdForUpdate(userWallet(shipper, TYPE_SHIPPER).getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        if (nz(w.getTotalBalance()).signum() < 0) {
            throw new AppException(ErrorCode.WALLET_HAS_DEBT, null, java.util.Map.of("balance", nz(w.getTotalBalance())));
        }
        BigDecimal required = nz(p.getRemainingAmount()).setScale(2, RoundingMode.HALF_UP);
        if (required.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal available = nz(w.getTotalBalance()).subtract(nz(w.getCodReserved())).max(BigDecimal.ZERO);
        if (available.compareTo(required) < 0) {
            throw new AppException(ErrorCode.SHIPPER_COD_BALANCE_INSUFFICIENT, null, java.util.Map.of(
                    "required", required, "available", available, "missing", required.subtract(available)));
        }
        reserve(w, required, WalletTransactionType.COD_RESERVED,
                "Đơn đặt trước " + p.getCode() + ": ký quỹ " + plain(required) + "đ tiền thu hộ, trả lại khi giao xong hoặc giao thất bại");
        return required;
    }

    /** Số dư ví shipper còn dùng được để ký quỹ tiền thu hộ (đang nợ hoặc chưa có ví = 0) */
    @Transactional(readOnly = true)
    public BigDecimal shipperAvailable(User shipper) {
        return walletRepository.findFirstByUser_IdAndType(shipper.getId(), TYPE_SHIPPER)
                .map(w -> nz(w.getTotalBalance()).subtract(nz(w.getCodReserved())).max(BigDecimal.ZERO))
                .orElse(BigDecimal.ZERO);
    }

    /** Gỡ ký quỹ của lượt giao hiện tại (giao xong / thất bại) */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseShipperCod(PreOrder p, String reason) {
        BigDecimal amount = nz(p.getCodReservedAmount());
        if (amount.signum() <= 0 || p.getShipper() == null) return;
        Wallet w = walletRepository.findByIdForUpdate(userWallet(p.getShipper(), TYPE_SHIPPER).getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        reserve(w, amount.negate(), WalletTransactionType.COD_RESERVE_RELEASED,
                "Đơn đặt trước " + p.getCode() + ": " + reason + ", gỡ ký quỹ " + plain(amount) + "đ");
    }

    /** Shipper đã cầm tiền mặt của người mua: trừ ví shipper đúng số đó (sàn đã ghi nhận ở recordRemainingCollected) */
    @Transactional(propagation = Propagation.MANDATORY)
    public void shipperCashCollected(PreOrder p) {
        BigDecimal cash = nz(p.getRemainingAmount());
        if (cash.signum() <= 0 || p.getShipper() == null) return;
        adjust(userWallet(p.getShipper(), TYPE_SHIPPER), BigDecimal.ZERO, cash.negate(), WalletTransactionType.COD_CASH_COLLECTED,
                "Đơn đặt trước " + p.getCode() + ": thu tiền mặt " + plain(cash) + "đ của người mua, trừ vào tiền ký quỹ");
    }

    /** Trả cước một lượt giao cho shipper: sàn trừ số dư, shipper nhận phần sau phí dịch vụ. Trả về số shipper nhận */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal payShipper(PreOrder p, BigDecimal gross, BigDecimal feeRate, String what) {
        if (gross == null || gross.signum() <= 0 || p.getShipper() == null) return BigDecimal.ZERO;
        BigDecimal fee = gross.multiply(feeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(fee);
        if (net.signum() <= 0) return BigDecimal.ZERO;
        adjust(platformWallet(), BigDecimal.ZERO, net.negate(), WalletTransactionType.CAMPAIGN_SHIPPER_PAYOUT,
                "Trả cước " + what + " đơn " + p.getCode() + " cho shipper #" + p.getShipper().getId()
                        + " (" + plain(gross) + "đ - phí dịch vụ " + plain(fee) + "đ)");
        adjust(userWallet(p.getShipper(), TYPE_SHIPPER), BigDecimal.ZERO, net, WalletTransactionType.SHIPPER_INCOME,
                "Cước " + what + " đơn đặt trước " + p.getCode() + ": " + plain(gross) + "đ - phí dịch vụ " + plain(fee) + "đ");
        return net;
    }

    /** Trả cước một chuyến xe chở hàng ra hub: sàn trừ số dư, tài xế nhận phần sau phí dịch vụ. Trả về số tài xế nhận */
    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal payHubTrip(User driver, String what, BigDecimal gross, BigDecimal feeRate) {
        if (gross == null || gross.signum() <= 0 || driver == null) return BigDecimal.ZERO;
        BigDecimal fee = gross.multiply(feeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(fee);
        if (net.signum() <= 0) return BigDecimal.ZERO;
        adjust(platformWallet(), BigDecimal.ZERO, net.negate(), WalletTransactionType.CAMPAIGN_SHIPPER_PAYOUT,
                "Trả cước xe chở hàng ra hub " + what + " cho tài xế #" + driver.getId() + " (" + plain(gross) + "đ" + (fee.signum() > 0 ? " - phí dịch vụ " + plain(fee) + "đ" : "") + ")");
        adjust(userWallet(driver, TYPE_SHIPPER), BigDecimal.ZERO, net, WalletTransactionType.SHIPPER_INCOME,
                "Cước xe chở hàng ra hub " + what + ": " + plain(gross) + "đ" + (fee.signum() > 0 ? " - phí dịch vụ " + plain(fee) + "đ" : ""));
        return net;
    }

    /** Báo hàng hư được duyệt: sàn lấy tiền từ ví sàn trả vào ví người mua */
    @Transactional(propagation = Propagation.MANDATORY)
    public void compensateDamage(PreOrder p, BigDecimal amount, BigDecimal kg) {
        if (amount == null || amount.signum() <= 0) return;
        Wallet platform = walletRepository.findByIdForUpdate(platformWallet().getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        if (nz(platform.getTotalBalance()).compareTo(amount) < 0) {
            throw new AppException(ErrorCode.PLATFORM_BALANCE_INSUFFICIENT);
        }
        String what = kg.stripTrailingZeros().toPlainString() + " kg hư, đơn " + p.getCode() + " (chiến dịch " + p.getCampaign().getCode() + ")";
        adjust(platform, BigDecimal.ZERO, amount.negate(), WalletTransactionType.CAMPAIGN_DAMAGE_COMPENSATION, "Bồi thường " + what);
        adjust(userWallet(p.getBuyer(), TYPE_BUYER), BigDecimal.ZERO, amount, WalletTransactionType.CAMPAIGN_DAMAGE_REFUND, "Hoàn tiền " + what);
    }

    /** Cộng/trừ frozen và số dư khả dụng của một ví, ghi 1 dòng sổ cái */
    private void adjust(Wallet wallet, BigDecimal frozenDelta, BigDecimal balanceDelta, WalletTransactionType type, String description) {
        Wallet locked = walletRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        BigDecimal frozen = nz(locked.getFrozenBalance()).add(frozenDelta).max(BigDecimal.ZERO);
        BigDecimal balance = nz(locked.getTotalBalance()).add(balanceDelta);
        locked.setFrozenBalance(frozen);
        locked.setTotalBalance(balance);
        if (type == WalletTransactionType.CAMPAIGN_FARM_INCOME || type == WalletTransactionType.SHIPPER_INCOME) {
            locked.setTotalRevenueAllTime(nz(locked.getTotalRevenueAllTime()).add(balanceDelta));
        }
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);

        BigDecimal amount = frozenDelta.signum() != 0 ? frozenDelta : balanceDelta;
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .type(type)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .balanceAfter(balance)
                .frozenAfter(frozen)
                .description(description.length() > 500 ? description.substring(0, 500) : description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /** Cộng/trừ tiền giữ chỗ (codReserved), số dư không đổi; ghi 1 dòng sổ cái */
    private void reserve(Wallet locked, BigDecimal delta, WalletTransactionType type, String description) {
        BigDecimal reserved = nz(locked.getCodReserved()).add(delta).max(BigDecimal.ZERO);
        locked.setCodReserved(reserved);
        locked.setUpdateAt(LocalDateTime.now());
        walletRepository.save(locked);
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(locked)
                .type(type)
                .amount(delta.negate().setScale(2, RoundingMode.HALF_UP))
                .balanceAfter(nz(locked.getTotalBalance()))
                .frozenAfter(nz(locked.getFrozenBalance()))
                .description(description.length() > 500 ? description.substring(0, 500) : description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private Wallet platformWallet() {
        return walletRepository.findFirstByType(TYPE_PLATFORM).orElseGet(() -> {
            User admin = userRepository.findByRole_NameIn(List.of("ADMIN")).stream()
                    .min(Comparator.comparingInt(User::getId))
                    .orElseThrow(() -> new AppException(ErrorCode.PLATFORM_WALLET_OWNER_MISSING));
            return userWallet(admin, TYPE_PLATFORM);
        });
    }

    private Wallet userWallet(User user, String type) {
        var found = walletRepository.findFirstByUser_IdAndType(user.getId(), type);
        if (found.isEmpty() && TYPE_BUYER.equals(type)) {
            found = walletRepository.findFirstByUser_IdAndType(user.getId(), TYPE_REFERRAL);
        }
        return found.orElseGet(() -> walletRepository.save(Wallet.builder()
                .user(user)
                .type(type)
                .status(WalletStatus.ACTIVE)
                .createAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .build()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
