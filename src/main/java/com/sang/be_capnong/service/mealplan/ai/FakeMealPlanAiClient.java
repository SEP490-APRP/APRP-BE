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

        String advice = "AI đã phân tích nội dung của bạn. Hệ thống sẽ tối ưu hóa thực đơn an toàn phù hợp với thể trạng.";
        List<String> suggestedOptions = new ArrayList<>();
        if (normText.contains("trao nguoc") || normText.contains("da day") || normText.contains("bao tu")) {
            advice = "Tình trạng trào ngược hoặc đau dạ dày cần hạn chế thực phẩm nhiều dầu mỡ, đồ quá chua hoặc cay nồng. Hệ thống đã lưu ý nhóm tiêu hóa nhạy cảm để loại trừ các món gây kích ứng đường ruột.";
            suggestedOptions.add("Đau dạ dày / Tiêu hóa nhạy cảm");
            suggestedOptions.add("Hạn chế đồ chua cay");
        }

        return ProfileExtraction.builder()
                .addConditions(addConditions)
                .removeConditions(removeConditions)
                .addAllergies(addAllergies)
                .removeAllergies(removeAllergies)
                .advice(advice)
                .notes("Phân tích tự động từ mô tả cá nhân")
                .suggestedOptions(suggestedOptions)
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

        // Farm Clustering: identify the dominant shop to minimize shipping fee
        Map<Integer, Integer> shopFrequency = new HashMap<>();
        for (CandidateComboDto c : in.getCandidates()) {
            if (c.getShopId() != null) {
                shopFrequency.put(c.getShopId(), shopFrequency.getOrDefault(c.getShopId(), 0) + 1);
            }
        }
        Integer preferredShopId = shopFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Map<MealType, List<CandidateComboDto>> byMealType = new HashMap<>();
        for (CandidateComboDto c : in.getCandidates()) {
            if (c.getMealType() != null) {
                byMealType.computeIfAbsent(c.getMealType(), k -> new ArrayList<>()).add(c);
            }
        }

        // Sort candidates prioritizing the preferred shop to optimize shipping
        if (preferredShopId != null) {
            for (List<CandidateComboDto> list : byMealType.values()) {
                list.sort((a, b) -> {
                    boolean aPref = Objects.equals(a.getShopId(), preferredShopId);
                    boolean bPref = Objects.equals(b.getShopId(), preferredShopId);
                    if (aPref && !bPref) return -1;
                    if (!aPref && bPref) return 1;
                    return 0;
                });
            }
        }

        Set<Integer> recentIds = in.getBuyer() != null && in.getBuyer().getRecentComboIds() != null
                ? new HashSet<>(in.getBuyer().getRecentComboIds())
                : Collections.emptySet();

        Set<Integer> usedInThisPlan = new HashSet<>();
        List<GeneratedPlan.GeneratedDay> generatedDays = new ArrayList<>();

        for (int d = 1; d <= days; d++) {
            List<GeneratedPlan.GeneratedMeal> meals = new ArrayList<>();
            for (MealType mt : mealTypes) {
                List<CandidateComboDto> available = byMealType.getOrDefault(mt, Collections.emptyList());
                if (available.isEmpty() && mt == MealType.DINNER) {
                    available = byMealType.getOrDefault(MealType.LUNCH, Collections.emptyList());
                }
                if (!available.isEmpty()) {
                    final int currentDay = d;
                    // Sort options dynamically based on shelf life, weekend feasibility, and non-repetition
                    List<CandidateComboDto> candidatePool = new ArrayList<>(available);
                    candidatePool.sort((c1, c2) -> {
                        // 1. Avoid recent combos from previous plans (Case 16: Non-repetition)
                        boolean c1Recent = recentIds.contains(c1.getComboId());
                        boolean c2Recent = recentIds.contains(c2.getComboId());
                        if (c1Recent && !c2Recent) return 1;
                        if (!c1Recent && c2Recent) return -1;

                        // 2. Avoid duplicates already used in this plan
                        boolean c1Used = usedInThisPlan.contains(c1.getComboId());
                        boolean c2Used = usedInThisPlan.contains(c2.getComboId());
                        if (c1Used && !c2Used) return 1;
                        if (!c1Used && c2Used) return -1;

                        String n1 = (c1.getName() != null ? c1.getName() : "").toLowerCase();
                        String n2 = (c2.getName() != null ? c2.getName() : "").toLowerCase();

                        // 3. Weekend feasibility (Case 15): Day >= 6 prefer celebratory, hotpot, or stew
                        if (currentDay >= 6) {
                            boolean c1Weekend = n1.contains("lẩu") || n1.contains("hầm") || n1.contains("tiệc");
                            boolean c2Weekend = n2.contains("lẩu") || n2.contains("hầm") || n2.contains("tiệc");
                            if (c1Weekend && !c2Weekend) return -1;
                            if (!c1Weekend && c2Weekend) return 1;
                        }

                        // 4. Shelf-life order (Case 14):
                        // Days 1-3: prefer leafy greens & fresh mushrooms
                        // Days 4-7: prefer root vegetables & durable gourds
                        if (currentDay <= 3) {
                            boolean c1Leafy = n1.contains("rau") || n1.contains("xà lách") || n1.contains("nấm") || n1.contains("mầm");
                            boolean c2Leafy = n2.contains("rau") || n2.contains("xà lách") || n2.contains("nấm") || n2.contains("mầm");
                            if (c1Leafy && !c2Leafy) return -1;
                            if (!c1Leafy && c2Leafy) return 1;
                        } else {
                            boolean c1Root = n1.contains("củ") || n1.contains("bí") || n1.contains("khoai") || n1.contains("bắp cải");
                            boolean c2Root = n2.contains("củ") || n2.contains("bí") || n2.contains("khoai") || n2.contains("bắp cải");
                            if (c1Root && !c2Root) return -1;
                            if (!c1Root && c2Root) return 1;
                        }

                        return 0;
                    });

                    int offset = (d - 1) * 3 + mt.ordinal();
                    CandidateComboDto chosen = candidatePool.get(Math.abs(offset) % candidatePool.size());
                    usedInThisPlan.add(chosen.getComboId());
                    int servingQuantity = Math.max(1, (int) Math.ceil((double) people / 3.0));
                    meals.add(GeneratedPlan.GeneratedMeal.builder()
                            .mealType(mt.name())
                            .items(List.of(GeneratedPlan.GeneratedItem.builder()
                                    .comboId(chosen.getComboId())
                                    .quantity(servingQuantity)
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
