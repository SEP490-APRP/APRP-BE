package com.sang.be_capnong.service.mealplan;

import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.entity.ProductCombo;
import com.sang.be_capnong.enums.DietPreference;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ConfigurableDietPolicyImpl implements DietPolicy {

    private static final List<String> MEAT_SEAFOOD_KEYWORDS = List.of(
            "thit", "ca", "tom", "muc", "cua", "nghieu", "so", "oc", "ghe",
            "bo", "heo", "ga", "vit", "cha lua", "xuc xich", "gan", "hai san"
    );

    private static final List<String> DAIRY_EGG_KEYWORDS = List.of(
            "trung", "sua", "pho mai", "bo sua", "mayonnaise"
    );

    @Override
    public boolean isCompatible(BuildCombo combo, DietPreference preference) {
        if (preference == null || preference == DietPreference.NON_VEGETARIAN) {
            return true;
        }

        if (combo == null) {
            return false;
        }

        // If combo type explicitly declares vegan/chay
        String comboType = stripAccents(combo.getType() != null ? combo.getType() : "");
        boolean taggedVegan = comboType.contains("chay") || comboType.contains("vegan");

        if (combo.getProductCombos() == null || combo.getProductCombos().isEmpty()) {
            return taggedVegan;
        }

        for (ProductCombo pc : combo.getProductCombos()) {
            if (pc.getProduct() == null) continue;

            String productName = stripAccents(pc.getProduct().getProductName());

            // Both VEGETARIAN and VEGAN exclude meat and seafood
            if (containsAnyWord(productName, MEAT_SEAFOOD_KEYWORDS)) {
                return false;
            }

            // VEGAN also excludes eggs and dairy
            if (preference == DietPreference.VEGAN) {
                if (containsAnyWord(productName, DAIRY_EGG_KEYWORDS)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean containsAnyWord(String text, List<String> keywords) {
        String padded = " " + text + " ";
        for (String kw : keywords) {
            if (padded.contains(" " + kw + " ") || padded.contains(" " + kw) || text.contains(kw)) {
                return true;
            }
        }
        return false;
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
