package com.sang.be_capnong.service.campaign;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Tính lại đơn khi hàng đạt chuẩn không đủ: giảm nguyên thùng sao cho ít người mua bị ảnh hưởng nhất.
 * <ul>
 * <li>Nếu một đơn đủ lớn để bù cả phần còn thiếu thì chỉ giảm đơn đó, chọn đơn bị cắt dư ít nhất, cùng mức thì đơn cọc sau.</li>
 * <li>Không đơn nào đủ thì huỷ đơn lớn nhất (cùng cỡ thì đơn cọc sau) rồi tính tiếp phần còn thiếu.</li>
 * </ul>
 */
public final class ShortfallPlanner {

    /** Một đơn chưa đóng gói: cỡ thùng, số thùng và lúc đặt cọc */
    public record Demand(int id, BigDecimal crateKg, int crates, LocalDateTime paidAt) {
        BigDecimal kg() {
            return crateKg.multiply(BigDecimal.valueOf(crates));
        }
    }

    private ShortfallPlanner() {
    }

    /** @return mã đơn bị giảm -> số thùng còn giữ (0 = huỷ đơn); đơn không đổi thì không có trong kết quả */
    public static Map<Integer, Integer> plan(List<Demand> orders, BigDecimal availableKg) {
        BigDecimal missing = orders.stream().map(Demand::kg).reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(availableKg.max(BigDecimal.ZERO));
        Map<Integer, Integer> keep = new LinkedHashMap<>();
        List<Demand> left = new ArrayList<>(orders);
        Comparator<Demand> laterFirst = Comparator.comparing((Demand d) -> d.paidAt() == null ? LocalDateTime.MAX : d.paidAt())
                .thenComparingInt(Demand::id).reversed();
        while (missing.signum() > 0 && !left.isEmpty()) {
            BigDecimal need = missing;
            Optional<Demand> covering = left.stream().filter(d -> d.kg().compareTo(need) >= 0)
                    .min(Comparator.comparing((Demand d) -> cutCrates(d, need).multiply(d.crateKg()).subtract(need))
                            .thenComparing(laterFirst));
            if (covering.isPresent()) {
                Demand d = covering.get();
                keep.put(d.id(), d.crates() - cutCrates(d, need).intValue());
                break;
            }
            Demand biggest = left.stream().min(Comparator.comparing(Demand::kg).reversed().thenComparing(laterFirst)).orElseThrow();
            keep.put(biggest.id(), 0);
            missing = missing.subtract(biggest.kg());
            left.remove(biggest);
        }
        return keep;
    }

    private static BigDecimal cutCrates(Demand d, BigDecimal missing) {
        return missing.divide(d.crateKg(), 0, RoundingMode.UP);
    }
}
