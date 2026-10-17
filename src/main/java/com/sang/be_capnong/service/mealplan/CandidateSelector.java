package com.sang.be_capnong.service.mealplan;

import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.DietPreference;
import com.sang.be_capnong.enums.MealType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CandidateSelector {

    DietPolicy dietPolicy;
    NutritionCalculator nutritionCalculator;

    public List<CandidateComboDto> selectCandidates(
            List<BuildCombo> allCombos,
            List<Allergen> buyerAllergies,
            DietPreference buyerDietPreference,
            Set<MealType> targetMealTypes,
            Map<Integer, NutritionInfo> nutritionMap,
            int maxCandidates
    ) {
        if (allCombos == null || allCombos.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> buyerAllergenIds = new HashSet<>();
        Set<String> buyerAllergenNames = new HashSet<>();
        if (buyerAllergies != null) {
            for (Allergen a : buyerAllergies) {
                if (a != null) {
                    buyerAllergenIds.add(a.getId());
                    buyerAllergenNames.add(stripAccents(a.getName()));
                }
            }
        }

        List<CandidateComboDto> candidates = new ArrayList<>();

        for (BuildCombo combo : allCombos) {
            // Rule 4: Purchasable check
            if (!"ACTIVE".equalsIgnoreCase(combo.getStatus())) {
                continue;
            }
            if (combo.getShop() == null || !Boolean.TRUE.equals(combo.getShop().getShopActive())) {
                continue;
            }
            int total = combo.getTotalQuantity() != null ? combo.getTotalQuantity() : 0;
            int sold = combo.getSoldQuantity() != null ? combo.getSoldQuantity() : 0;
            int stockLeft = total - sold;
            if (stockLeft <= 0) {
                continue;
            }

            // Rule 8: Meal type match
            if (targetMealTypes != null && !targetMealTypes.isEmpty()) {
                if (combo.getMealType() == null || !targetMealTypes.contains(combo.getMealType())) {
                    continue;
                }
            }

            // Rule 2: Diet Preference filter
            if (!dietPolicy.isCompatible(combo, buyerDietPreference)) {
                continue;
            }

            // Rule 1: Deterministic Allergy filter
            boolean hasAllergenConflict = false;
            boolean allergenDataIncomplete = false;
            List<String> comboIngredientNames = new ArrayList<>();
            List<String> comboAllergenNames = new ArrayList<>();

            if (combo.getProductCombos() != null) {
                for (ProductCombo pc : combo.getProductCombos()) {
                    Product product = pc.getProduct();
                    if (product == null) continue;

                    comboIngredientNames.add(product.getProductName());

                    List<ProductAllergen> productAllergens = product.getProductAllergens();
                    if (productAllergens == null || productAllergens.isEmpty()) {
                        allergenDataIncomplete = true;
                    } else {
                        for (ProductAllergen pa : productAllergens) {
                            if (pa.getAllergen() != null) {
                                String aName = pa.getAllergen().getName();
                                comboAllergenNames.add(aName);
                                if (buyerAllergenIds.contains(pa.getAllergen().getId()) ||
                                        buyerAllergenNames.contains(stripAccents(aName))) {
                                    hasAllergenConflict = true;
                                    break;
                                }
                            }
                        }
                    }

                    // Fallback keyword check on product name against buyer allergens
                    String normProductName = stripAccents(product.getProductName());
                    for (String allergenName : buyerAllergenNames) {
                        if (!allergenName.isBlank() && normProductName.contains(allergenName)) {
                            hasAllergenConflict = true;
                            break;
                        }
                    }

                    if (hasAllergenConflict) {
                        break;
                    }
                }
            }

            if (hasAllergenConflict) {
                continue; // Hard skip: non-negotiable safety constraint
            }

            // Calculate nutrition
            NutritionSummary nutrition = nutritionCalculator.calculateComboNutrition(combo, nutritionMap);

            CandidateComboDto dto = CandidateComboDto.builder()
                    .comboId(combo.getComboId())
                    .name(combo.getComboName())
                    .mealType(combo.getMealType())
                    .price(combo.getPrice())
                    .region(combo.getRegion())
                    .shopId(combo.getShop() != null ? combo.getShop().getId() : null)
                    .shopName(combo.getShop() != null ? combo.getShop().getShopName() : null)
                    .ingredients(comboIngredientNames)
                    .allergens(comboAllergenNames)
                    .nutrition(nutrition)
                    .stockLeft(stockLeft)
                    .allergenDataIncomplete(allergenDataIncomplete)
                    .build();

            candidates.add(dto);
        }

        // Cap to maxCandidates
        if (candidates.size() > maxCandidates && maxCandidates > 0) {
            return candidates.subList(0, maxCandidates);
        }

        return candidates;
    }

    private String stripAccents(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .replace("đ", "d").replace("Đ", "D")
                .toLowerCase().trim();
    }
}
