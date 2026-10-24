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
            "thit", "hai san", "xuc xich", "cha lua", "cha ca", "gio lua", "lap xuong",
            "thit bo", "thit heo", "thit ga", "thit vit", "thit ca",
            "ca hoi", "ca thu", "ca chep", "ca basa", "ca loc", "ca dieu hong", "ca bien", "ca dong",
            "nghieu", "cua dong", "cua bien", "tom kho", "tom tuoi", "muc tuoi", "muc kho"
    );

    private static final List<String> DAIRY_EGG_KEYWORDS = List.of(
            "trung ga", "trung vit", "trung cut", "sua bo", "sua dac", "pho mai", "bo sua", "mayonnaise"
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
        if (taggedVegan) {
            return true;
        }

        if (combo.getProductCombos() == null || combo.getProductCombos().isEmpty()) {
            return true;
        }

        for (ProductCombo pc : combo.getProductCombos()) {
            if (pc.getProduct() == null) continue;

            String productName = stripAccents(pc.getProduct().getProductName());

            // Check if product is explicitly meat or seafood
            if (isMeatOrSeafood(productName)) {
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

    private boolean isMeatOrSeafood(String productName) {
        // Safe produce that often contains overlapping letters ("ca chua", "ca rot", "ca tim", "cai", "bong cai", "trai bo")
        if (productName.contains("ca chua") || productName.contains("ca rot") ||
                productName.contains("ca tim") || productName.contains("cai ") ||
                productName.contains("cai") || productName.contains("cu cai") ||
                productName.contains("trai bo") || productName.contains("qua bo") ||
                productName.contains("bong cai") || productName.contains("rau ")) {
            // Still check if it explicitly contains meat keywords like "thit", "hai san"
            return productName.contains("thit") || productName.contains("hai san");
        }

        return containsAnyWord(productName, MEAT_SEAFOOD_KEYWORDS);
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
