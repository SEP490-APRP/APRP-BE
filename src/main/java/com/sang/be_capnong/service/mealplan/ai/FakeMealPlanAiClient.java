package com.sang.be_capnong.service.mealplan.ai;

import com.sang.be_capnong.enums.MealType;
import com.sang.be_capnong.service.mealplan.CandidateComboDto;
import com.sang.be_capnong.service.mealplan.GeneratedPlan;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("fakeMealPlanAiClient")
public class FakeMealPlanAiClient implements MealPlanAiClient {

    @Override
    public ProfileExtraction extractProfile(ExtractionInput in) {
        String text = in.getText() != null ? in.getText() : "";
        String normText = stripAccents(text);

        List<String> addConditions = new ArrayList<>();
        List<String> removeConditions = new ArrayList<>();
        List<String> addAllergies = new ArrayList<>();
        List<String> removeAllergies = new ArrayList<>();

        boolean isRemoval = normText.contains("het") || normText.contains("khong con") || normText.contains("khoi");

        if (in.getKnownConditions() != null) {
            for (String condition : in.getKnownConditions()) {
                String normCondition = stripAccents(condition);
                if (normText.contains(normCondition)) {
                    if (isRemoval && (normText.contains("het " + normCondition) || normText.contains("khoi " + normCondition))) {
                        removeConditions.add(condition);
                    } else {
                        addConditions.add(condition);
                    }
                }
            }
        }

        if (in.getKnownAllergens() != null) {
            for (String allergen : in.getKnownAllergens()) {
                String normAllergen = stripAccents(allergen);
                if (normText.contains(normAllergen) ||
                        (normAllergen.contains("dau phong") && (normText.contains("lac") || normText.contains("dau phong")))) {
                    if (isRemoval && (normText.contains("het di ung " + normAllergen) || normText.contains("khong con di ung"))) {
                        removeAllergies.add(allergen);
                    } else {
                        addAllergies.add(allergen);
                    }
                }
            }
        }

        // Plan request extraction
        ExtractedPlanRequest planRequest = null;
        Integer days = extractNumber(normText, "(\\d+)\\s*(ngay|day)");
        Integer people = extractNumber(normText, "(\\d+)\\s*(nguoi|person)");

        if (days != null || people != null || normText.contains("thuc don") || normText.contains("ke hoach")) {
            planRequest = ExtractedPlanRequest.builder()
                    .numberOfDays(days != null ? days : 7)
                    .numberOfPeople(people)
                    .goal(normText.contains("giam can") ? "giảm cân" : "ăn ngon khỏe")
                    .mealTypes(List.of("BREAKFAST", "LUNCH", "DINNER"))
                    .build();
        }

        return ProfileExtraction.builder()
                .addConditions(addConditions)
                .removeConditions(removeConditions)
                .addAllergies(addAllergies)
                .removeAllergies(removeAllergies)
                .planRequest(planRequest)
                .needsClarification(null)
                .build();
    }

    @Override
    public GeneratedPlan generatePlan(GenerationInput in) {
        int days = in.getConstraints() != null ? in.getConstraints().getNumberOfDays() : 7;
        int people = in.getConstraints() != null && in.getConstraints().getNumberOfPeople() > 0
                ? in.getConstraints().getNumberOfPeople()
                : 2;

        List<MealType> mealTypes = in.getConstraints() != null && in.getConstraints().getMealTypes() != null
                ? in.getConstraints().getMealTypes()
                : List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER);

        Map<MealType, List<CandidateComboDto>> byMealType = new HashMap<>();
        for (CandidateComboDto c : in.getCandidates()) {
            if (c.getMealType() != null) {
                byMealType.computeIfAbsent(c.getMealType(), k -> new ArrayList<>()).add(c);
            }
        }

        List<GeneratedPlan.GeneratedDay> generatedDays = new ArrayList<>();

        for (int d = 1; d <= days; d++) {
            List<GeneratedPlan.GeneratedMeal> meals = new ArrayList<>();
            for (MealType mt : mealTypes) {
                List<CandidateComboDto> available = byMealType.getOrDefault(mt, Collections.emptyList());
                if (!available.isEmpty()) {
                    // Pick combo rotating by day to ensure diversity
                    CandidateComboDto chosen = available.get((d - 1) % available.size());
                    meals.add(GeneratedPlan.GeneratedMeal.builder()
                            .mealType(mt.name())
                            .items(List.of(GeneratedPlan.GeneratedItem.builder()
                                    .comboId(chosen.getComboId())
                                    .quantity(people)
                                    .build()))
                            .build());
                }
            }
            generatedDays.add(GeneratedPlan.GeneratedDay.builder()
                    .dayIndex(d)
                    .meals(meals)
                    .build());
        }

        return GeneratedPlan.builder()
                .days(generatedDays)
                .notes("Thực đơn mẫu cân đối dinh dưỡng từ nông sản chợ CapNong")
                .build();
    }

    private Integer extractNumber(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
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
