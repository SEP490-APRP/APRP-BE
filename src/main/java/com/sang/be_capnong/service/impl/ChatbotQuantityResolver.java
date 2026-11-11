package com.sang.be_capnong.service.impl;

import java.util.Locale;
import java.util.Set;

/**
 * Quy đổi số lượng khách nói ("500g", "2 ký") sang số lượng theo đơn vị bán của sản phẩm.
 * Giỏ hàng chỉ nhận số nguyên từ 1 đến 999, nên trường hợp không quy đổi được sẽ bị từ chối
 * thay vì tự làm tròn: thêm sai số lượng vào giỏ tệ hơn là hỏi lại khách.
 */
final class ChatbotQuantityResolver {

    static final int MAX_QUANTITY = 999;

    private static final Set<String> KILOGRAM = Set.of("kg", "ký", "kí", "ki", "kilo", "kilogram", "cân");
    private static final Set<String> GRAM = Set.of("g", "gr", "gam", "gram");
    private static final Set<String> HECTOGRAM = Set.of("lạng", "hg");

    private ChatbotQuantityResolver() {
    }

    /** @return số lượng hợp lệ để thêm vào giỏ; null khi không quy đổi được. */
    static Integer resolve(Double requested, String requestedUnit, String productUnit) {
        double amount = requested != null ? requested : 1d;
        Double kilograms = toKilograms(amount, requestedUnit);

        double quantity;
        if (isBlank(requestedUnit)) {
            quantity = amount;
        } else if (kilograms != null) {
            // Khách nói theo khối lượng thì sản phẩm phải bán theo kg mới quy đổi được.
            if (toKilograms(1d, productUnit) == null || toKilograms(1d, productUnit) != 1d) return null;
            quantity = kilograms;
        } else if (normalize(requestedUnit).equals(normalize(productUnit))) {
            quantity = amount;
        } else {
            return null;
        }

        long rounded = Math.round(quantity);
        if (Math.abs(quantity - rounded) > 1e-9 || rounded < 1 || rounded > MAX_QUANTITY) return null;
        return (int) rounded;
    }

    private static Double toKilograms(double amount, String unit) {
        String normalized = normalize(unit);
        if (KILOGRAM.contains(normalized)) return amount;
        if (HECTOGRAM.contains(normalized)) return amount / 10d;
        if (GRAM.contains(normalized)) return amount / 1000d;
        return null;
    }

    private static String normalize(String unit) {
        return unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
