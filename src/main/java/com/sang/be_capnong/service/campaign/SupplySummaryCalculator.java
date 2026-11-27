package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.dto.response.campaign.SupplySummaryResponse;
import com.sang.be_capnong.entity.CampaignFarmRegistration;
import com.sang.be_capnong.entity.PreOrder;
import com.sang.be_capnong.enums.PreOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Câu chuyện hàng tại điểm tập kết: người mua đặt bao nhiêu, nhà vườn giao, hư hỏng, thiếu sau khi cân,
 * được bổ sung bao nhiêu và phần không bù được đã bớt khỏi đơn.
 * Luôn khớp: đặt = đạt chuẩn đợt đầu + thiếu sau cân (khi thiếu), thiếu sau cân = bổ sung + đã bớt đơn + còn thiếu.
 */
public final class SupplySummaryCalculator {

    /** Lý do huỷ đơn do chốt phần thiếu bắt đầu bằng cụm này */
    public static final String SHORTFALL_REASON = "Thiếu hàng tại điểm tập kết";
    /** Ghi chú cân của các lần bổ sung cũ (trước khi lưu riêng số kg) */
    private static final Pattern SUPPLEMENT_NOTE = Pattern.compile("Bổ sung ([0-9]+(?:\\.[0-9]+)?) kg");

    private SupplySummaryCalculator() {
    }

    /**
     * @param lots      các lô đã duyệt
     * @param orders    đơn đã cọc trở đi, kèm đơn đã huỷ
     * @param topUpAt   lúc mở kêu gọi bổ sung (lô đăng ký sau lúc này là hàng bổ sung)
     * @param countedKg số kg đạt chuẩn được tính của một lô
     */
    public static SupplySummaryResponse summarize(List<CampaignFarmRegistration> lots, List<PreOrder> orders, LocalDateTime topUpAt,
            Function<CampaignFarmRegistration, BigDecimal> countedKg) {
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal rejected = BigDecimal.ZERO;
        BigDecimal accepted = BigDecimal.ZERO;
        BigDecimal supplement = BigDecimal.ZERO;
        Map<String, BigDecimal> bySupplier = new LinkedHashMap<>();
        for (CampaignFarmRegistration r : lots) {
            if (r.getIntakeAt() == null) continue;
            BigDecimal counted = nz(countedKg.apply(r));
            received = received.add(nz(r.getReceivedWeightKg()));
            rejected = rejected.add(nz(r.getRejectedWeightKg()));
            accepted = accepted.add(counted);
            BigDecimal extra = supplementOf(r, topUpAt, counted).min(counted);
            if (extra.signum() > 0) {
                supplement = supplement.add(extra);
                String name = r.getShop() == null ? "Nhà vườn"
                        : r.getShop().getShopName() != null ? r.getShop().getShopName() : r.getShop().getFullName();
                bySupplier.merge(name, extra, BigDecimal::add);
            }
        }

        BigDecimal sold = BigDecimal.ZERO;
        BigDecimal cut = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        int reduced = 0;
        int cancelled = 0;
        for (PreOrder p : orders) {
            boolean shortCancel = p.getStatus() == PreOrderStatus.CANCELLED
                    && p.getCancelReason() != null && p.getCancelReason().startsWith(SHORTFALL_REASON);
            if (p.getStatus() == PreOrderStatus.CANCELLED && !shortCancel) continue;
            if (p.getStatus() == PreOrderStatus.PENDING) continue;
            BigDecimal crate = p.getCrateSize().kgDecimal();
            int original = p.getOriginalCrateQuantity() != null ? p.getOriginalCrateQuantity() : p.getCrateQuantity();
            if (shortCancel) {
                cut = cut.add(crate.multiply(BigDecimal.valueOf(original)));
                cancelled++;
            } else {
                sold = sold.add(nz(p.getTotalKg()));
                if (original > p.getCrateQuantity()) {
                    cut = cut.add(crate.multiply(BigDecimal.valueOf(original - p.getCrateQuantity())));
                    reduced++;
                }
            }
            refund = refund.add(nz(p.getShortfallRefundAmount()));
        }
        BigDecimal ordered = sold.add(cut);
        BigDecimal firstAccepted = accepted.subtract(supplement).max(BigDecimal.ZERO);
        return SupplySummaryResponse.builder()
                .orderedKg(ordered)
                .deliveredByFarmsKg(received.subtract(supplement).max(BigDecimal.ZERO))
                .rejectedKg(rejected)
                .firstAcceptedKg(firstAccepted)
                .shortAfterWeighingKg(ordered.subtract(firstAccepted).max(BigDecimal.ZERO))
                .supplementKg(supplement)
                .supplements(bySupplier.entrySet().stream().map(e -> new SupplySummaryResponse.Supplier(e.getKey(), e.getValue())).toList())
                .cutKg(cut)
                .reducedOrderCount(reduced)
                .cancelledOrderCount(cancelled)
                .refundAmount(refund)
                .remainingShortKg(sold.subtract(accepted).max(BigDecimal.ZERO))
                .build();
    }

    private static BigDecimal supplementOf(CampaignFarmRegistration r, LocalDateTime topUpAt, BigDecimal counted) {
        if (r.getSupplementKg() != null) return r.getSupplementKg();
        BigDecimal noted = notedSupplement(r.getIntakeNote());
        if (noted.signum() > 0) return noted;
        boolean topUpLot = topUpAt != null && r.getCreatedAt() != null && r.getCreatedAt().isAfter(topUpAt);
        return topUpLot ? counted : BigDecimal.ZERO;
    }

    /** Tổng kg bổ sung ghi trong ghi chú cân */
    public static BigDecimal notedSupplement(String note) {
        BigDecimal total = BigDecimal.ZERO;
        if (note != null) {
            Matcher m = SUPPLEMENT_NOTE.matcher(note);
            while (m.find()) total = total.add(new BigDecimal(m.group(1)));
        }
        return total;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
