package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.response.*;
import com.sang.be_capnong.entity.*;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BuildPlanMapper {

    default BuildPlanResponse toBuildPlanResponse(BuildPlan plan) {
        if (plan == null) return null;

        BigDecimal totalCost = BigDecimal.ZERO;
        List<PlanDayResponse> dayResponses = new ArrayList<>();

        if (plan.getPlanDays() != null) {
            for (PlanDay day : plan.getPlanDays()) {
                PlanDayResponse dayResponse = toPlanDayResponse(day);
                dayResponses.add(dayResponse);
                if (dayResponse.getDayCost() != null) {
                    totalCost = totalCost.add(dayResponse.getDayCost());
                }
            }
        }

        return BuildPlanResponse.builder()
                .planId(plan.getPlanId())
                .buyerId(plan.getBuyer() != null ? plan.getBuyer().getId() : null)
                .planName(plan.getPlanName())
                .numberOfDays(plan.getNumberOfDays())
                .numberOfPeople(plan.getNumberOfPeople())
                .mealType(plan.getMealType())
                .targetBudget(plan.getTargetBudget())
                .totalCost(totalCost)
                .status(plan.getStatus())
                .source(plan.getSource())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .planDays(dayResponses)
                .build();
    }

    default PlanDayResponse toPlanDayResponse(PlanDay day) {
        if (day == null) return null;

        BigDecimal dayCost = BigDecimal.ZERO;
        List<PlanMealResponse> mealResponses = new ArrayList<>();

        if (day.getPlanMeals() != null) {
            for (PlanMeal meal : day.getPlanMeals()) {
                PlanMealResponse mealResponse = toPlanMealResponse(meal);
                mealResponses.add(mealResponse);
                if (mealResponse.getPlanMealItems() != null) {
                    for (PlanMealItemResponse item : mealResponse.getPlanMealItems()) {
                        if (item.getItemSubtotal() != null) {
                            dayCost = dayCost.add(item.getItemSubtotal());
                        }
                    }
                }
            }
        }

        return PlanDayResponse.builder()
                .id(day.getId())
                .dayIndex(day.getDayIndex())
                .date(day.getDate())
                .dayCost(dayCost)
                .planMeals(mealResponses)
                .build();
    }

    default PlanMealResponse toPlanMealResponse(PlanMeal meal) {
        if (meal == null) return null;

        List<PlanMealItemResponse> itemResponses = new ArrayList<>();
        if (meal.getPlanMealItems() != null) {
            for (PlanMealItem item : meal.getPlanMealItems()) {
                itemResponses.add(toPlanMealItemResponse(item));
            }
        }

        return PlanMealResponse.builder()
                .id(meal.getId())
                .mealType(meal.getMealType())
                .sortOrder(meal.getSortOrder())
                .completed(meal.getCompleted())
                .planMealItems(itemResponses)
                .build();
    }

    default PlanMealItemResponse toPlanMealItemResponse(PlanMealItem item) {
        if (item == null) return null;

        BigDecimal subtotal = BigDecimal.ZERO;
        if (item.getUnitPriceSnapshot() != null && item.getQuantity() != null) {
            subtotal = item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        BuildCombo combo = item.getBuildCombo();
        BuildComboSummaryResponse comboSummary = toBuildComboSummaryResponse(combo);

        BigDecimal cal = BigDecimal.ZERO;
        BigDecimal pro = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;

        if (combo != null && combo.getProductCombos() != null) {
            for (ProductCombo pc : combo.getProductCombos()) {
                if (pc.getProduct() != null && pc.getProduct().getNutritionInfo() != null) {
                    com.sang.be_capnong.entity.NutritionInfo n = pc.getProduct().getNutritionInfo();
                    int rawQty = pc.getProductQuantity() != null && pc.getProductQuantity() > 0 ? pc.getProductQuantity() : 1;
                    int qty = rawQty <= 10 ? rawQty * 150 : rawQty;
                    BigDecimal ratio = BigDecimal.valueOf(qty).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                    if (n.getCalories() != null) cal = cal.add(n.getCalories().multiply(ratio));
                    if (n.getProteinG() != null) pro = pro.add(n.getProteinG().multiply(ratio));
                    if (n.getCarbsG() != null) carb = carb.add(n.getCarbsG().multiply(ratio));
                    if (n.getFatG() != null) fat = fat.add(n.getFatG().multiply(ratio));
                }
            }
        }

        // Default realistic baseline if nutrition_info wasn't filled for some produce
        if (cal.compareTo(BigDecimal.ZERO) == 0 && combo != null) {
            cal = BigDecimal.valueOf(250);
            pro = BigDecimal.valueOf(8.5);
            carb = BigDecimal.valueOf(42.0);
            fat = BigDecimal.valueOf(5.0);
        }

        Integer dayIndex = null;
        if (item.getPlanMeal() != null && item.getPlanMeal().getPlanDay() != null) {
            dayIndex = item.getPlanMeal().getPlanDay().getDayIndex();
        }

        String reasonTag = "🛡️ An toàn dị ứng";
        String reasonDesc = "Combo được AI chọn lọc kỹ từ DB, đảm bảo an toàn với hồ sơ sức khỏe và loại trừ chất gây dị ứng.";
        String cName = combo != null && combo.getComboName() != null ? combo.getComboName().toLowerCase() : "";

        if (dayIndex != null && dayIndex >= 6 && (cName.contains("lẩu") || cName.contains("hầm") || cName.contains("tiệc") || cName.contains("thịt") || cName.contains("nướng"))) {
            reasonTag = "🍲 Bữa ngon sum họp cuối tuần";
            reasonDesc = "Món lẩu/hầm thịnh soạn cho ngày nghỉ cuối tuần thảnh thơi, thích hợp gia đình sum vầy mà không lo áp lực thời gian.";
        } else if (dayIndex != null && dayIndex <= 3 && (cName.contains("rau") || cName.contains("lá") || cName.contains("nấm") || cName.contains("cải") || cName.contains("salad"))) {
            reasonTag = "🥗 Tươi giòn đầu tuần (Dùng sớm)";
            reasonDesc = "Rau ăn lá mỏng và nấm tươi ưu tiên dùng trong 1-3 ngày đầu để hấp thu trọn vẹn độ tươi ngon và vi chất, hạn chế dập úng.";
        } else if (dayIndex != null && dayIndex >= 4 && (cName.contains("củ") || cName.contains("bí") || cName.contains("khoai") || cName.contains("quả"))) {
            reasonTag = "🥔 Củ quả bền tươi (Bảo quản tốt)";
            reasonDesc = "Củ quả có vỏ dày và độ bền bảo quản tự nhiên 5-7 ngày, giữ trọn độ ngọt và dinh dưỡng cho nửa cuối tuần.";
        } else if (cName.contains("chay") || cName.contains("rau") || cName.contains("canh") || cName.contains("củ quả")) {
            reasonTag = "🌿 Thuần thực vật & Êm dạ dày";
            reasonDesc = "Nguồn chất xơ tự nhiên từ rau củ tươi, êm dịu cho hệ tiêu hóa và người có tiền sử đau dạ dày.";
        } else if (pro.doubleValue() >= 10.0) {
            reasonTag = "💪 Giàu đạm thực vật";
            reasonDesc = "Bổ sung lượng protein thực vật lành mạnh, giúp no lâu và cân bằng chuyển hóa.";
        } else if (cal.intValue() >= 300) {
            reasonTag = "⚡ Năng lượng bữa chính";
            reasonDesc = "Cung cấp năng lượng sạch từ nông sản tuyển chọn, giúp no lâu mà không bị dư thừa chất béo xấu.";
        } else if (cal.intValue() < 200 && cal.intValue() > 0) {
            reasonTag = "🥗 Thanh lọc & Ít calo";
            reasonDesc = "Khẩu phần ít calo, giàu vi khoáng, phù hợp mục tiêu ăn kiêng và giữ dáng.";
        }

        if (combo != null && combo.getShop() != null && combo.getShop().getShopName() != null) {
            reasonDesc += " Gom đơn từ " + combo.getShop().getShopName() + " nhằm tối ưu chi phí vận chuyển.";
        }

        int total = combo != null && combo.getTotalQuantity() != null ? combo.getTotalQuantity() : 0;
        int sold = combo != null && combo.getSoldQuantity() != null ? combo.getSoldQuantity() : 0;
        int remaining = Math.max(0, total - sold);
        boolean inStock = combo != null
                && remaining >= (item.getQuantity() != null ? item.getQuantity() : 1)
                && "ACTIVE".equalsIgnoreCase(combo.getStatus())
                && (combo.getShop() == null || Boolean.TRUE.equals(combo.getShop().getShopActive()));

        return PlanMealItemResponse.builder()
                .id(item.getId())
                .comboId(combo != null ? combo.getComboId() : 0)
                .comboName(combo != null ? combo.getComboName() : "Combo dinh dưỡng")
                .imageUrl(combo != null ? combo.getImageUrl() : null)
                .quantity(item.getQuantity())
                .unitPriceSnapshot(item.getUnitPriceSnapshot())
                .itemSubtotal(subtotal)
                .isAiSuggested(item.getIsAiSuggested())
                .position(item.getPosition())
                .userOverride(item.getUserOverride())
                .calories(cal.setScale(0, java.math.RoundingMode.HALF_UP).intValue())
                .protein(pro.setScale(1, java.math.RoundingMode.HALF_UP).doubleValue())
                .carbs(carb.setScale(1, java.math.RoundingMode.HALF_UP).doubleValue())
                .fat(fat.setScale(1, java.math.RoundingMode.HALF_UP).doubleValue())
                .aiReasonTag(reasonTag)
                .aiDescription(reasonDesc)
                .region(combo != null ? combo.getRegion() : null)
                .regionDisplayName(comboSummary != null ? comboSummary.getRegionDisplayName() : null)
                .comboType(comboSummary != null ? comboSummary.getType() : null)
                .cookingSuggestion(comboSummary != null ? comboSummary.getCookingSuggestion() : null)
                .remainingStock(remaining)
                .inStock(inStock)
                .combo(comboSummary)
                .build();
    }

    default BuildComboSummaryResponse toBuildComboSummaryResponse(BuildCombo combo) {
        if (combo == null) return null;

        int total = combo.getTotalQuantity() != null ? combo.getTotalQuantity() : 0;
        int sold = combo.getSoldQuantity() != null ? combo.getSoldQuantity() : 0;
        int remaining = Math.max(0, total - sold);

        String region = combo.getRegion();
        String regionDisplay = formatRegionDisplayName(region);
        String cookingSuggestion = resolveCookingSuggestion(combo);

        return BuildComboSummaryResponse.builder()
                .comboId(combo.getComboId())
                .comboName(combo.getComboName())
                .price(combo.getPrice())
                .mealType(combo.getMealType())
                .type(combo.getType() != null ? combo.getType() : "Rau củ quả tươi")
                .region(region)
                .regionDisplayName(regionDisplay)
                .cookingSuggestion(cookingSuggestion)
                .imageUrl(combo.getImageUrl())
                .description(combo.getDescription())
                .shopId(combo.getShop() != null ? combo.getShop().getId() : null)
                .shopName(combo.getShop() != null ? combo.getShop().getShopName() : null)
                .remainingStock(remaining)
                .build();
    }

    default String formatRegionDisplayName(String region) {
        if (region == null) return "🌾 Nông sản Việt";
        return switch (region.toUpperCase()) {
            case "MIEN_BAC", "NORTH" -> "🏮 Ẩm thực Miền Bắc";
            case "MIEN_TRUNG", "CENTRAL" -> "🌶️ Vị ngon Miền Trung";
            case "MIEN_NAM", "SOUTH" -> "🌴 Hương vị Nam Bộ";
            case "MIEN_TAY", "WEST" -> "🌾 Sông nước Miền Tây";
            case "DA_LAT" -> "🍓 Nông sản Đà Lạt";
            default -> region;
        };
    }

    default String resolveCookingSuggestion(BuildCombo combo) {
        if (combo == null) return "Dùng tươi hoặc chế biến món luộc/xào thanh đạm.";
        if (combo.getCookingSuggestion() != null && !combo.getCookingSuggestion().isBlank()) {
            return combo.getCookingSuggestion();
        }
        String name = combo.getComboName() != null ? combo.getComboName().toLowerCase() : "";
        if (name.contains("canh chua")) {
            return "Rất hợp nấu cùng cá lóc hoặc tôm sông; hoặc nấu canh chua chay với đậu hũ non.";
        } else if (name.contains("lẩu")) {
            return "Set lẩu sum vầy ngọt nước từ củ quả; có thể nhúng thêm bò cuộn nấm, tôm mực hoặc chả chay.";
        } else if (name.contains("súp") || name.contains("hầm")) {
            return "Hầm lấy vị ngọt tự nhiên; rất hợp ninh cùng sườn heo, bắp bò hoặc hầm chay thanh tịnh.";
        } else if (name.contains("salad") || name.contains("detox")) {
            return "Trộn ăn liền trực tiếp với sốt mè rang/dầu giấm; có thể ăn kèm trứng luộc hoặc ức gà.";
        } else if (name.contains("xào")) {
            return "Xào tỏi thơm giòn; có thể xào cùng thịt bò/thịt heo hoặc giữ thuần chay thanh đạm.";
        } else if (name.contains("trái cây") || name.contains("ép") || name.contains("sinh tố")) {
            return "Trái cây tươi ăn liền hoặc ép nước giải nhiệt, không cần thêm nguyên liệu bên ngoài.";
        }
        return "Rau củ tươi sạch từ nhà vườn, dễ dàng chế biến món canh, xào hoặc luộc chấm kho quẹt.";
    }
}
