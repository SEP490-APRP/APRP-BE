package com.sang.be_capnong.service.mealplan;

import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.entity.NutritionInfo;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductCombo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
public class NutritionCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100.0");

    public NutritionSummary calculateComboNutrition(BuildCombo combo, Map<Integer, NutritionInfo> nutritionMap) {
        if (combo == null || combo.getProductCombos() == null || combo.getProductCombos().isEmpty()) {
            return NutritionSummary.builder().incomplete(true).build();
        }

        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalCarbs = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        boolean incomplete = false;

        for (ProductCombo pc : combo.getProductCombos()) {
            Product product = pc.getProduct();
            if (product == null) {
                incomplete = true;
                continue;
            }

            NutritionInfo nutrition = nutritionMap != null
                    ? nutritionMap.get(product.getProductId())
                    : product.getNutritionInfo();

            if (nutrition == null) {
                incomplete = true;
                continue;
            }

            // productQuantity in grams (e.g. 200g). If stored as pack units (<= 10, e.g. 1 pack), convert to 150g per unit
            int rawQty = (pc.getProductQuantity() != null && pc.getProductQuantity() > 0) ? pc.getProductQuantity() : 1;
            int qtyGrams = rawQty <= 10 ? rawQty * 150 : rawQty;

            BigDecimal ratio = BigDecimal.valueOf(qtyGrams).divide(HUNDRED, 4, RoundingMode.HALF_UP);

            if (nutrition.getCalories() != null) {
                totalCalories = totalCalories.add(nutrition.getCalories().multiply(ratio));
            }
            if (nutrition.getProteinG() != null) {
                totalProtein = totalProtein.add(nutrition.getProteinG().multiply(ratio));
            }
            if (nutrition.getCarbsG() != null) {
                totalCarbs = totalCarbs.add(nutrition.getCarbsG().multiply(ratio));
            }
            if (nutrition.getFatG() != null) {
                totalFat = totalFat.add(nutrition.getFatG().multiply(ratio));
            }
        }

        return NutritionSummary.builder()
                .calories(totalCalories.setScale(1, RoundingMode.HALF_UP))
                .proteinG(totalProtein.setScale(1, RoundingMode.HALF_UP))
                .carbsG(totalCarbs.setScale(1, RoundingMode.HALF_UP))
                .fatG(totalFat.setScale(1, RoundingMode.HALF_UP))
                .incomplete(incomplete)
                .build();
    }
}
