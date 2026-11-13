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

    /** Cộng/trừ frozen và số dư khả dụng của một ví, ghi 1 dòng sổ cái */
    private void adjust(Wallet wallet, BigDecimal frozenDelta, BigDecimal balanceDelta, WalletTransactionType type, String description) {
        Wallet locked = walletRepository.findByIdForUpdate(wallet.getWalletId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        BigDecimal frozen = nz(locked.getFrozenBalance()).add(frozenDelta).max(BigDecimal.ZERO);
        BigDecimal balance = nz(locked.getTotalBalance()).add(balanceDelta);
        locked.setFrozenBalance(frozen);
        locked.setTotalBalance(balance);
        if (type == WalletTransactionType.CAMPAIGN_FARM_INCOME) {
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
