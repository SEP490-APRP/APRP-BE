package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.*;
import com.sang.be_capnong.dto.response.BuildPlanResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.PlanMealResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.MealType;
import com.sang.be_capnong.enums.PlanSource;
import com.sang.be_capnong.enums.PlanStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.BuildPlanMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.BuildPlanService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BuildPlanServiceImpl implements BuildPlanService {

    BuildPlanRepository buildPlanRepository;
    PlanDayRepository planDayRepository;
    PlanMealRepository planMealRepository;
    PlanMealItemRepository planMealItemRepository;
    BuildComboRepository buildComboRepository;
    UserRepository userRepository;
    BuildPlanMapper buildPlanMapper;
    com.sang.be_capnong.service.mealplan.DietPolicy dietPolicy;
    com.sang.be_capnong.service.mealplan.CandidateSelector candidateSelector;
    com.sang.be_capnong.service.mealplan.PlanValidator planValidator;
    com.sang.be_capnong.service.mealplan.ai.MealPlanAiClient mealPlanAiClient;
    @org.springframework.beans.factory.annotation.Qualifier("fakeMealPlanAiClient")
    com.sang.be_capnong.service.mealplan.ai.MealPlanAiClient fallbackAiClient;
    PlanGenerationRequestRepository planGenerationRequestRepository;
    UserAllergyRepository userAllergyRepository;
    UserHealthConditionRepository userHealthConditionRepository;
    NutritionInfoRepository nutritionInfoRepository;
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    com.sang.be_capnong.service.CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public com.sang.be_capnong.dto.response.PlanPreCheckResponse preCheckPlan(com.sang.be_capnong.dto.request.PlanPreCheckRequest request) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        int days = request.getNumberOfDays() != null && request.getNumberOfDays() > 0 ? request.getNumberOfDays() : 7;
        int people = request.getNumberOfPeople() != null && request.getNumberOfPeople() > 0 ? request.getNumberOfPeople() : 2;
        int combosPerMeal = Math.max(1, (int) Math.ceil((double) people / 3.0));

        int mealsPerDay = (request.getMealTypes() != null && !request.getMealTypes().isEmpty())
                ? request.getMealTypes().size()
                : 3;
        int totalMeals = days * mealsPerDay;
        int totalCombosNeeded = totalMeals * combosPerMeal;

        // Fetch active combos
        List<BuildCombo> allActiveCombos = buildComboRepository.findAll().stream()
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .filter(c -> c.getShop() != null && Boolean.TRUE.equals(c.getShop().getShopActive()))
                .filter(c -> (c.getTotalQuantity() != null ? c.getTotalQuantity() : 0) - (c.getSoldQuantity() != null ? c.getSoldQuantity() : 0) > 0)
                .toList();

        // Filter compatible with diet preference if any
        if (buyer.getDietPreference() != null) {
            allActiveCombos = allActiveCombos.stream()
                    .filter(c -> dietPolicy.isCompatible(c, buyer.getDietPreference()))
                    .toList();
        }

        double minPrice = 60000.0;
        double avgPrice = 75000.0;
        if (!allActiveCombos.isEmpty()) {
            minPrice = allActiveCombos.stream()
                    .mapToDouble(c -> c.getPrice() != null ? c.getPrice().doubleValue() : 70000.0)
                    .min()
                    .orElse(60000.0);
            avgPrice = allActiveCombos.stream()
                    .mapToDouble(c -> c.getPrice() != null ? c.getPrice().doubleValue() : 75000.0)
                    .average()
                    .orElse(75000.0);
        }

        BigDecimal estimatedMinCost = BigDecimal.valueOf(Math.round(totalCombosNeeded * minPrice));
        BigDecimal estimatedStandardCost = BigDecimal.valueOf(Math.round(totalCombosNeeded * avgPrice));

        boolean hasPortionWarning = people <= 2;
        String portionAdvice = hasPortionWarning
                ? String.format("Các combo nông sản được đóng gói định lượng cho mâm cơm 3–4 người (~1kg rau củ). Với khẩu phần %d người, mỗi combo sẽ dôi dư khoảng 40–50%%. Bạn có thể nấu 1 combo chia làm 2 bữa (ví dụ trưa và tối) hoặc bảo quản tủ lạnh.", people)
                : null;

        BigDecimal budget = request.getTargetBudget();
        boolean hasBudgetWarning = false;
        String budgetAdvice = null;
        boolean isFeasible = true;

        boolean hasStockShortage = false;
        String stockShortageMessage = null;
        if (allActiveCombos.isEmpty()) {
            hasStockShortage = true;
            isFeasible = false;
            stockShortageMessage = "Kho nông sản hiện tại chưa có combo rau củ nào sẵn sàng để lên thực đơn (các nhà vườn đang chuẩn bị vụ thu hoạch mới). Bạn vui lòng quay lại sau ít phút!";
        } else if (allActiveCombos.size() < totalCombosNeeded) {
            hasStockShortage = true;
            stockShortageMessage = String.format("Kho nông sản hiện có %d combo còn hàng, trong khi thực đơn %d ngày (%d bữa) cần tối thiểu %d combo. Bạn có thể giảm số ngày hoặc chọn ít bữa hơn để đảm bảo đủ nguyên liệu tươi.",
                    allActiveCombos.size(), days, totalMeals, totalCombosNeeded);
        }

        if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
            if (budget.compareTo(estimatedMinCost) < 0) {
                hasBudgetWarning = true;
                isFeasible = false;
                budgetAdvice = String.format("Ngân sách %,dđ cho %d người ăn %d ngày (%d bữa) thấp hơn mức chi phí tối thiểu của nông sản sạch (khoảng %,dđ – %,dđ).",
                        budget.longValue(), people, days, totalMeals, estimatedMinCost.longValue(), estimatedStandardCost.longValue());
            }
        }

        List<com.sang.be_capnong.dto.response.PlanPreCheckResponse.ScenarioOption> scenarios = new ArrayList<>();
        if (!isFeasible || hasPortionWarning || hasStockShortage) {
            // Scenario 1: Tối ưu 2 bữa chính (Trưa & Tối), bữa sáng tự túc
            int optMeals = days * 2;
            int optCombos = optMeals * combosPerMeal;
            BigDecimal optCost = BigDecimal.valueOf(Math.round(optCombos * avgPrice));
            scenarios.add(com.sang.be_capnong.dto.response.PlanPreCheckResponse.ScenarioOption.builder()
                    .scenarioId("OPTIMIZE_MEALS")
                    .scenarioKey("OPTIMIZE_MEALS")
                    .title("Tối ưu 2 bữa chính (Trưa & Tối)")
                    .description("Lên thực đơn cho bữa Trưa và Tối đầy đủ món canh/xào, bữa sáng gia đình tự túc nhẹ nhàng.")
                    .estimatedCost(optCost)
                    .adjustedDays(days)
                    .suggestedDays(days)
                    .adjustedPeople(people)
                    .adjustedMealTypes(List.of("LUNCH", "DINNER"))
                    .suggestedMealTypes(List.of("LUNCH", "DINNER"))
                    .adjustedBudget(optCost)
                    .suggestedBudget(optCost)
                    .build());

            // Scenario 2: Rút ngắn số ngày cho vừa ngân sách
            int reducedDays = budget != null && budget.compareTo(BigDecimal.ZERO) > 0
                    ? Math.max(2, (int) Math.floor(budget.doubleValue() / (mealsPerDay * combosPerMeal * avgPrice)))
                    : 3;
            if (reducedDays >= days) reducedDays = Math.max(2, days - 2);
            int reducedCombos = reducedDays * mealsPerDay * combosPerMeal;
            BigDecimal reducedCost = BigDecimal.valueOf(Math.round(reducedCombos * avgPrice));
            List<String> currentMealTypeNames = request.getMealTypes() != null
                    ? request.getMealTypes().stream().map(Enum::name).toList()
                    : List.of("BREAKFAST", "LUNCH", "DINNER");

            scenarios.add(com.sang.be_capnong.dto.response.PlanPreCheckResponse.ScenarioOption.builder()
                    .scenarioId("REDUCE_DAYS")
                    .scenarioKey("REDUCE_DAYS")
                    .title("Rút ngắn còn " + reducedDays + " ngày trọn vẹn")
                    .description(String.format("Giữ trọn vẹn %d bữa/ngày với thực đơn tươi ngon chuẩn ngân sách của bạn.", mealsPerDay))
                    .estimatedCost(reducedCost)
                    .adjustedDays(reducedDays)
                    .suggestedDays(reducedDays)
                    .adjustedPeople(people)
                    .adjustedMealTypes(currentMealTypeNames)
                    .suggestedMealTypes(currentMealTypeNames)
                    .adjustedBudget(reducedCost)
                    .suggestedBudget(reducedCost)
                    .build());

            // Scenario 3: Giữ nguyên số ngày và nâng ngân sách lên mức thực tế
            scenarios.add(com.sang.be_capnong.dto.response.PlanPreCheckResponse.ScenarioOption.builder()
                    .scenarioId("REALISTIC_BUDGET")
                    .scenarioKey("REALISTIC_BUDGET")
                    .title(String.format("Giữ đủ %d ngày theo chi phí thực tế", days))
                    .description("Điều chỉnh ngân sách lên mức tiêu chuẩn để cả nhà có thực đơn phong phú, đa dạng món mỗi ngày.")
                    .estimatedCost(estimatedStandardCost)
                    .adjustedDays(days)
                    .suggestedDays(days)
                    .adjustedPeople(people)
                    .adjustedMealTypes(currentMealTypeNames)
                    .suggestedMealTypes(currentMealTypeNames)
                    .adjustedBudget(estimatedStandardCost)
                    .suggestedBudget(estimatedStandardCost)
                    .build());
        }

        return com.sang.be_capnong.dto.response.PlanPreCheckResponse.builder()
                .isFeasible(isFeasible && !hasStockShortage)
                .hasPortionWarning(hasPortionWarning)
                .hasBudgetWarning(hasBudgetWarning)
                .hasStockShortage(hasStockShortage)
                .stockShortageMessage(stockShortageMessage)
                .numberOfDays(days)
                .numberOfPeople(people)
                .targetBudget(budget)
                .estimatedMinCost(estimatedMinCost)
                .estimatedStandardCost(estimatedStandardCost)
                .combosPerMeal(combosPerMeal)
                .totalMeals(totalMeals)
                .totalCombosNeeded(totalCombosNeeded)
                .portionAdvice(portionAdvice)
                .budgetAdvice(budgetAdvice)
                .suggestedScenarios(scenarios)
                .scenarios(scenarios)
                .build();
    }

    @Override
    @Transactional
    public BuildPlanResponse generateAiPlan(PlanGenerateRequest request) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        log.info("Buyer {} generating AI meal plan: days={}, people={}, budget={}",
                buyerId, request.getNumberOfDays(), request.getNumberOfPeople(), request.getTargetBudget());

        // 1. Fetch confirmed profile data
        List<Allergen> buyerAllergies = userAllergyRepository.findByBuyer_Id(buyerId).stream()
                .map(UserAllergy::getAllergen)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<String> buyerConditions = userHealthConditionRepository.findByBuyer_Id(buyerId).stream()
                .map(uc -> uc.getCondition().getName())
                .filter(java.util.Objects::nonNull)
                .toList();

        List<Integer> recentComboIds = buildPlanRepository.findTop5ByBuyer_IdOrderByPlanIdDesc(buyerId).stream()
                .flatMap(p -> p.getPlanDays() != null ? p.getPlanDays().stream() : java.util.stream.Stream.empty())
                .flatMap(d -> d.getPlanMeals() != null ? d.getPlanMeals().stream() : java.util.stream.Stream.empty())
                .flatMap(m -> m.getPlanMealItems() != null ? m.getPlanMealItems().stream() : java.util.stream.Stream.empty())
                .filter(item -> item.getBuildCombo() != null)
                .map(item -> item.getBuildCombo().getComboId())
                .distinct()
                .toList();

        // 2. Audit log entry (plan_id = null initially)
        java.util.Map<String, Object> inputParamsMap = java.util.Map.of(
                "request", request,
                "buyerAllergies", buyerAllergies.stream().map(Allergen::getName).toList(),
                "buyerConditions", buyerConditions,
                "dietPreference", buyer.getDietPreference() != null ? buyer.getDietPreference().name() : "NON_VEGETARIAN",
                "recentComboIds", recentComboIds
        );

        String inputJson = "";
        try {
            inputJson = objectMapper.writeValueAsString(inputParamsMap);
        } catch (Exception e) {
            log.warn("Failed to serialize input params for audit: {}", e.getMessage());
        }

        PlanGenerationRequest audit = PlanGenerationRequest.builder()
                .buyer(buyer)
                .requestedAt(LocalDateTime.now())
                .inputParams(inputJson)
                .build();
        audit = planGenerationRequestRepository.save(audit);

        // 3. Candidate selection
        List<BuildCombo> allCombos = buildComboRepository.findAll();
        List<Integer> allProductIds = allCombos.stream()
                .filter(c -> c.getProductCombos() != null)
                .flatMap(c -> c.getProductCombos().stream())
                .filter(pc -> pc.getProduct() != null)
                .map(pc -> pc.getProduct().getProductId())
                .distinct()
                .toList();

        java.util.Map<Integer, NutritionInfo> nutritionMap = nutritionInfoRepository.findByProduct_ProductIdIn(allProductIds).stream()
                .filter(n -> n.getProduct() != null)
                .collect(java.util.stream.Collectors.toMap(n -> n.getProduct().getProductId(), n -> n, (a, b) -> a));

        java.util.Set<MealType> targetMealTypes = request.getMealTypes() != null
                ? new java.util.HashSet<>(request.getMealTypes())
                : java.util.Set.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER);

        List<com.sang.be_capnong.service.mealplan.CandidateComboDto> candidates = candidateSelector.selectCandidates(
                allCombos,
                buyerAllergies,
                buyer.getDietPreference(),
                targetMealTypes,
                nutritionMap,
                60
        );

        if (candidates.isEmpty()) {
            log.warn("No suitable combos found for buyer {} constraints", buyerId);
            String specificReason = "Kho nông sản hiện không có đủ combo rau củ còn hàng phù hợp với các tiêu chí (ngân sách, khẩu phần hoặc loại bữa) bạn đã chọn.";
            if (allCombos.isEmpty()) {
                specificReason = "Hiện tại kho nông sản chưa có combo rau củ nào trên hệ thống. Vui lòng quay lại sau ít phút!";
            } else {
                long activeInStock = allCombos.stream()
                        .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                        .filter(c -> c.getShop() != null && Boolean.TRUE.equals(c.getShop().getShopActive()))
                        .filter(c -> (c.getTotalQuantity() != null ? c.getTotalQuantity() : 0) - (c.getSoldQuantity() != null ? c.getSoldQuantity() : 0) > 0)
                        .count();
                if (activeInStock == 0) {
                    specificReason = "Tất cả combo rau củ trên sàn hiện đang tạm hết hàng. Nhà vườn đang thu hoạch vụ mùa mới, bạn vui lòng quay lại sau nhé!";
                } else if (buyerAllergies != null && !buyerAllergies.isEmpty()) {
                    specificReason = "Các combo rau củ còn hàng trong kho đều chứa thành phần nằm trong danh mục dị ứng trong hồ sơ sức khỏe của bạn. Bạn hãy thử kiểm tra lại hồ sơ dị ứng hoặc chọn các bữa ăn khác!";
                } else if (buyer.getDietPreference() != null) {
                    specificReason = "Không tìm thấy combo rau củ còn hàng nào phù hợp với chế độ ăn " + buyer.getDietPreference() + " và các bữa ăn bạn chọn.";
                }
            }
            throw new AppException(ErrorCode.NO_SUITABLE_COMBOS, specificReason);
        }

        java.util.Map<Integer, com.sang.be_capnong.service.mealplan.CandidateComboDto> candidateMap = candidates.stream()
                .collect(java.util.stream.Collectors.toMap(com.sang.be_capnong.service.mealplan.CandidateComboDto::getComboId, c -> c));

        java.util.Map<Integer, BuildCombo> comboEntityMap = allCombos.stream()
                .collect(java.util.stream.Collectors.toMap(BuildCombo::getComboId, c -> c));

        // 4. Call AI (Contract B)
        com.sang.be_capnong.service.mealplan.ai.GenerationInput aiInput = com.sang.be_capnong.service.mealplan.ai.GenerationInput.builder()
                .constraints(com.sang.be_capnong.service.mealplan.ai.GenerationInput.Constraints.builder()
                        .numberOfDays(request.getNumberOfDays())
                        .numberOfPeople(request.getNumberOfPeople())
                        .targetBudget(request.getTargetBudget())
                        .mealTypes(request.getMealTypes())
                        .goal(request.getGoal())
                        .startDate(request.getStartDate())
                        .build())
                .buyer(com.sang.be_capnong.service.mealplan.ai.GenerationInput.BuyerContext.builder()
                        .dietPreference(buyer.getDietPreference())
                        .conditions(buyerConditions)
                        .allergies(buyerAllergies.stream().map(Allergen::getName).toList())
                        .recentComboIds(recentComboIds)
                        .build())
                .candidates(candidates)
                .build();

        com.sang.be_capnong.service.mealplan.GeneratedPlan planOutput = mealPlanAiClient.generatePlan(aiInput);

        // Record raw AI response in audit log
        try {
            audit.setRawAiResponse(objectMapper.writeValueAsString(planOutput));
            planGenerationRequestRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to serialize raw AI response: {}", e.getMessage());
        }

        // 5. Validate AI output
        com.sang.be_capnong.service.mealplan.ValidationResult validationResult = planValidator.validate(
                planOutput,
                candidateMap,
                request.getNumberOfDays(),
                request.getTargetBudget(),
                buyerAllergies
        );

        // Fail-soft: If AI output fails validation, fallback to deterministic heuristic generator
        if (!validationResult.isValid()) {
            log.warn("AI generated plan failed validation: {}, falling back to deterministic heuristic", validationResult.getErrors());
            planOutput = fallbackAiClient.generatePlan(aiInput);
            validationResult = planValidator.validate(
                    planOutput,
                    candidateMap,
                    request.getNumberOfDays(),
                    request.getTargetBudget(),
                    buyerAllergies
            );
        }

        // 6. Persist plan tree in one transaction
        String planTitle = request.getPlanName() != null && !request.getPlanName().isBlank()
                ? request.getPlanName().trim()
                : "Thực đơn AI (" + (request.getGoal() != null ? request.getGoal() : request.getNumberOfDays() + " ngày") + ")";

        BuildPlan plan = BuildPlan.builder()
                .buyer(buyer)
                .planName(planTitle)
                .numberOfDays(request.getNumberOfDays())
                .numberOfPeople(request.getNumberOfPeople())
                .mealType(request.getGoal())
                .targetBudget(request.getTargetBudget())
                .status(PlanStatus.AI_DRAFT)
                .source(PlanSource.AI)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .planDays(new ArrayList<>())
                .build();

        if (planOutput.getDays() != null) {
            for (com.sang.be_capnong.service.mealplan.GeneratedPlan.GeneratedDay gDay : planOutput.getDays()) {
                int dayIndex = gDay.getDayIndex() != null ? gDay.getDayIndex() : 1;
                PlanDay day = PlanDay.builder()
                        .dayIndex(dayIndex)
                        .date(request.getStartDate() != null ? request.getStartDate().plusDays(dayIndex - 1) : null)
                        .buildPlan(plan)
                        .planMeals(new ArrayList<>())
                        .build();

                int sortOrder = 1;
                if (gDay.getMeals() != null) {
                    for (com.sang.be_capnong.service.mealplan.GeneratedPlan.GeneratedMeal gMeal : gDay.getMeals()) {
                        MealType mt = MealType.valueOf(gMeal.getMealType().toUpperCase().trim());
                        if (targetMealTypes != null && !targetMealTypes.isEmpty() && !targetMealTypes.contains(mt)) {
                            continue; // Skip any meal type not requested by buyer
                        }
                        PlanMeal meal = PlanMeal.builder()
                                .mealType(mt)
                                .sortOrder(sortOrder++)
                                .completed(false)
                                .planDay(day)
                                .planMealItems(new ArrayList<>())
                                .build();

                        int pos = 1;
                        if (gMeal.getItems() != null) {
                            for (com.sang.be_capnong.service.mealplan.GeneratedPlan.GeneratedItem gItem : gMeal.getItems()) {
                                BuildCombo combo = comboEntityMap.get(gItem.getComboId());
                                if (combo != null) {
                                    int people = request.getNumberOfPeople() != null && request.getNumberOfPeople() > 0 ? request.getNumberOfPeople() : 1;
                                    int combosNeeded = Math.max(1, (int) Math.ceil((double) people / 3.0));
                                    int finalQty = (gItem.getQuantity() != null && gItem.getQuantity() > 0 && gItem.getQuantity() <= combosNeeded + 1)
                                            ? gItem.getQuantity()
                                            : combosNeeded;

                                    PlanMealItem item = PlanMealItem.builder()
                                            .planMeal(meal)
                                            .buildCombo(combo)
                                            .quantity(finalQty)
                                            .unitPriceSnapshot(combo.getPrice())
                                            .isAiSuggested(true)
                                            .userOverride(false)
                                            .position(pos++)
                                            .build();
                                    meal.getPlanMealItems().add(item);
                                }
                            }
                        }
                        day.getPlanMeals().add(meal);
                    }
                }
                plan.getPlanDays().add(day);
            }
        }

        BuildPlan savedPlan = buildPlanRepository.save(plan);

        // Link audit record with saved plan
        audit.setBuildPlan(savedPlan);
        planGenerationRequestRepository.save(audit);

        BuildPlanResponse response = buildPlanMapper.toBuildPlanResponse(savedPlan);
        if (validationResult.getWarnings() != null && !validationResult.getWarnings().isEmpty()) {
            response.setWarnings(validationResult.getWarnings());
        }

        return response;
    }

    @Override
    @Transactional
    public BuildPlanResponse createManualPlan(ManualPlanCreateRequest request) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        BuildPlan plan = BuildPlan.builder()
                .buyer(buyer)
                .planName(request.getPlanName() != null && !request.getPlanName().isBlank()
                        ? request.getPlanName().trim()
                        : "Thực đơn tuần " + (request.getStartDate() != null ? request.getStartDate().toString() : ""))
                .numberOfDays(request.getNumberOfDays())
                .numberOfPeople(request.getNumberOfPeople())
                .mealType(request.getMealType())
                .targetBudget(request.getTargetBudget())
                .status(PlanStatus.DRAFT)
                .source(PlanSource.MANUAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .planDays(new ArrayList<>())
                .build();

        for (int dayIndex = 1; dayIndex <= request.getNumberOfDays(); dayIndex++) {
            PlanDay day = PlanDay.builder()
                    .dayIndex(dayIndex)
                    .date(request.getStartDate() != null ? request.getStartDate().plusDays(dayIndex - 1) : null)
                    .buildPlan(plan)
                    .planMeals(new ArrayList<>())
                    .build();

            int sortOrder = 1;
            for (MealType mt : request.getMealTypes()) {
                PlanMeal meal = PlanMeal.builder()
                        .mealType(mt)
                        .sortOrder(sortOrder++)
                        .completed(false)
                        .planDay(day)
                        .planMealItems(new ArrayList<>())
                        .build();
                day.getPlanMeals().add(meal);
            }

            plan.getPlanDays().add(day);
        }

        BuildPlan savedPlan = buildPlanRepository.save(plan);
        return buildPlanMapper.toBuildPlanResponse(savedPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public BuildPlanResponse getPlanById(int planId) {
        BuildPlan plan = findPlanAndCheckOwnership(planId);
        return buildPlanMapper.toBuildPlanResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BuildPlanResponse> getMyPlans(PlanStatus status, int page, int size) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "planId"));

        Page<BuildPlan> pageData = (status != null)
                ? buildPlanRepository.findByBuyer_IdAndStatus(buyerId, status, pageable)
                : buildPlanRepository.findByBuyer_Id(buyerId, pageable);

        List<BuildPlanResponse> content = pageData.getContent().stream()
                .map(buildPlanMapper::toBuildPlanResponse)
                .toList();

        return PageResponse.from(pageData, content);
    }

    @Override
    @Transactional
    public BuildPlanResponse updatePlanHeader(int planId, PlanHeaderUpdateRequest request) {
        BuildPlan plan = findPlanAndCheckOwnership(planId);

        if (request.getPlanName() != null) {
            plan.setPlanName(request.getPlanName().trim());
        }
        if (request.getTargetBudget() != null) {
            plan.setTargetBudget(request.getTargetBudget());
        }
        if (request.getMealType() != null) {
            plan.setMealType(request.getMealType().trim());
        }
        plan.setUpdatedAt(LocalDateTime.now());

        BuildPlan saved = buildPlanRepository.save(plan);
        return buildPlanMapper.toBuildPlanResponse(saved);
    }

    @Override
    @Transactional
    public void deletePlan(int planId) {
        BuildPlan plan = findPlanAndCheckOwnership(planId);
        if (plan.getStatus() != PlanStatus.DRAFT && plan.getStatus() != PlanStatus.AI_DRAFT) {
            throw new AppException(ErrorCode.PLAN_CANNOT_DELETE_CONFIRMED);
        }
        buildPlanRepository.delete(plan);
    }

    @Override
    @Transactional
    public BuildPlanResponse confirmPlan(int planId) {
        BuildPlan plan = findPlanAndCheckOwnership(planId);
        if (plan.getStatus() == PlanStatus.CONFIRMED) {
            return buildPlanMapper.toBuildPlanResponse(plan); // Idempotent
        }
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            throw new AppException(ErrorCode.PLAN_ALREADY_ARCHIVED);
        }
        plan.setStatus(PlanStatus.CONFIRMED);
        plan.setUpdatedAt(LocalDateTime.now());
        BuildPlan saved = buildPlanRepository.save(plan);
        return buildPlanMapper.toBuildPlanResponse(saved);
    }

    @Override
    @Transactional
    public BuildPlanResponse archivePlan(int planId) {
        BuildPlan plan = findPlanAndCheckOwnership(planId);
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            return buildPlanMapper.toBuildPlanResponse(plan); // Idempotent
        }
        plan.setStatus(PlanStatus.ARCHIVED);
        plan.setUpdatedAt(LocalDateTime.now());
        BuildPlan saved = buildPlanRepository.save(plan);
        return buildPlanMapper.toBuildPlanResponse(saved);
    }

    @Override
    @Transactional
    public PlanMealResponse toggleMealCompleted(int mealId, boolean completed) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        PlanMeal meal = planMealRepository.findByIdAndPlanDay_BuildPlan_Buyer_Id(mealId, buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));

        BuildPlan plan = meal.getPlanDay().getBuildPlan();
        if (plan.getStatus() != PlanStatus.CONFIRMED) {
            throw new AppException(ErrorCode.PLAN_NOT_CONFIRMED);
        }

        meal.setCompleted(completed);
        PlanMeal saved = planMealRepository.save(meal);
        return buildPlanMapper.toPlanMealResponse(saved);
    }

    @Override
    @Transactional
    public PlanMealResponse addMealItem(int planId, int mealId, AddMealItemRequest request) {
        PlanMeal meal = findMealInPlanAndCheckOwnership(planId, mealId);
        BuildCombo combo = validatePurchasableCombo(request.getComboId(), meal.getMealType(), request.getQuantity());

        int nextPosition = (meal.getPlanMealItems() != null ? meal.getPlanMealItems().size() : 0) + 1;

        PlanMealItem item = PlanMealItem.builder()
                .planMeal(meal)
                .buildCombo(combo)
                .quantity(request.getQuantity())
                .unitPriceSnapshot(combo.getPrice())
                .isAiSuggested(false)
                .userOverride(false)
                .position(nextPosition)
                .build();

        if (meal.getPlanMealItems() == null) {
            meal.setPlanMealItems(new ArrayList<>());
        }
        meal.getPlanMealItems().add(item);
        planMealItemRepository.save(item);

        meal.getPlanDay().getBuildPlan().setUpdatedAt(LocalDateTime.now());
        return buildPlanMapper.toPlanMealResponse(meal);
    }

    @Override
    @Transactional
    public PlanMealResponse replaceMealItem(int planId, int mealId, int itemId, ReplaceMealItemRequest request) {
        PlanMeal meal = findMealInPlanAndCheckOwnership(planId, mealId);
        PlanMealItem existingItem = meal.getPlanMealItems().stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        int quantity = request.getQuantity() != null ? request.getQuantity() : existingItem.getQuantity();
        BuildCombo newCombo = validatePurchasableCombo(request.getNewComboId(), meal.getMealType(), quantity);

        existingItem.setBuildCombo(newCombo);
        existingItem.setQuantity(quantity);
        existingItem.setUnitPriceSnapshot(newCombo.getPrice());

        if (Boolean.TRUE.equals(existingItem.getIsAiSuggested())) {
            existingItem.setUserOverride(true);
        }

        planMealItemRepository.save(existingItem);
        meal.getPlanDay().getBuildPlan().setUpdatedAt(LocalDateTime.now());
        return buildPlanMapper.toPlanMealResponse(meal);
    }

    @Override
    @Transactional
    public PlanMealResponse removeMealItem(int planId, int mealId, int itemId) {
        PlanMeal meal = findMealInPlanAndCheckOwnership(planId, mealId);
        PlanMealItem itemToRemove = meal.getPlanMealItems().stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        meal.getPlanMealItems().remove(itemToRemove);
        planMealItemRepository.delete(itemToRemove);

        // Re-index remaining positions
        for (int i = 0; i < meal.getPlanMealItems().size(); i++) {
            meal.getPlanMealItems().get(i).setPosition(i + 1);
        }
        planMealItemRepository.saveAll(meal.getPlanMealItems());

        meal.getPlanDay().getBuildPlan().setUpdatedAt(LocalDateTime.now());
        return buildPlanMapper.toPlanMealResponse(meal);
    }

    @Override
    @Transactional
    public PlanMealResponse updateMealItemQuantity(int planId, int mealId, int itemId, int quantity) {
        PlanMeal meal = findMealInPlanAndCheckOwnership(planId, mealId);
        PlanMealItem item = meal.getPlanMealItems().stream()
                .filter(i -> i.getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        validatePurchasableCombo(item.getBuildCombo().getComboId(), meal.getMealType(), quantity);

        item.setQuantity(quantity);
        if (Boolean.TRUE.equals(item.getIsAiSuggested())) {
            item.setUserOverride(true);
        }

        planMealItemRepository.save(item);
        meal.getPlanDay().getBuildPlan().setUpdatedAt(LocalDateTime.now());
        return buildPlanMapper.toPlanMealResponse(meal);
    }

    private BuildPlan findPlanAndCheckOwnership(int planId) {
        BuildPlan plan = buildPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        if (!SecurityUtils.isAdmin()) {
            Integer currentUserId = SecurityUtils.getCurrentUserId();
            if (plan.getBuyer() == null || plan.getBuyer().getId() != currentUserId) {
                throw new AppException(ErrorCode.PLAN_FORBIDDEN);
            }
        }
        return plan;
    }

    private PlanMeal findMealInPlanAndCheckOwnership(int planId, int mealId) {
        BuildPlan plan = findPlanAndCheckOwnership(planId);
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            throw new AppException(ErrorCode.PLAN_ALREADY_ARCHIVED);
        }
        return plan.getPlanDays().stream()
                .flatMap(d -> d.getPlanMeals().stream())
                .filter(m -> m.getId() == mealId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND));
    }

    private BuildCombo validatePurchasableCombo(int comboId, MealType expectedMealType, int requiredQuantity) {
        BuildCombo combo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(combo.getStatus())) {
            throw new AppException(ErrorCode.COMBO_UNAVAILABLE);
        }

        if (combo.getShop() == null || !Boolean.TRUE.equals(combo.getShop().getShopActive())) {
            throw new AppException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        int total = combo.getTotalQuantity() != null ? combo.getTotalQuantity() : 0;
        int sold = combo.getSoldQuantity() != null ? combo.getSoldQuantity() : 0;
        if ((total - sold) < requiredQuantity) {
            throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
        }

        if (expectedMealType != null && combo.getMealType() != null) {
            boolean isCompatible = (combo.getMealType() == expectedMealType)
                    || (expectedMealType == MealType.LUNCH && combo.getMealType() == MealType.DINNER)
                    || (expectedMealType == MealType.DINNER && combo.getMealType() == MealType.LUNCH);
            if (!isCompatible) {
                throw new AppException(ErrorCode.MEAL_TYPE_MISMATCH);
            }
        }

        return combo;
    }

    @Override
    @Transactional(readOnly = true)
    public com.sang.be_capnong.dto.response.ShoppingListResponse getShoppingList(int planId) {
        BuildPlan plan = findPlanAndCheckOwnership(planId);

        java.util.Map<Integer, Integer> quantityByComboId = new java.util.LinkedHashMap<>();
        if (plan.getPlanDays() != null) {
            for (PlanDay day : plan.getPlanDays()) {
                if (day.getPlanMeals() != null) {
                    for (PlanMeal meal : day.getPlanMeals()) {
                        if (meal.getPlanMealItems() != null) {
                            for (PlanMealItem item : meal.getPlanMealItems()) {
                                if (item.getBuildCombo() != null) {
                                    int cId = item.getBuildCombo().getComboId();
                                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                                    quantityByComboId.put(cId, quantityByComboId.getOrDefault(cId, 0) + qty);
                                }
                            }
                        }
                    }
                }
            }
        }

        java.math.BigDecimal totalEstimatedPrice = java.math.BigDecimal.ZERO;
        boolean allInStock = true;
        java.util.List<com.sang.be_capnong.dto.response.ShoppingListItemResponse> items = new java.util.ArrayList<>();

        for (java.util.Map.Entry<Integer, Integer> entry : quantityByComboId.entrySet()) {
            int comboId = entry.getKey();
            int totalQty = entry.getValue();

            BuildCombo combo = buildComboRepository.findById(comboId).orElse(null);
            if (combo == null) continue;

            int total = combo.getTotalQuantity() != null ? combo.getTotalQuantity() : 0;
            int sold = combo.getSoldQuantity() != null ? combo.getSoldQuantity() : 0;
            int stockAvailable = Math.max(0, total - sold);
            boolean inStock = stockAvailable >= totalQty && "ACTIVE".equalsIgnoreCase(combo.getStatus());

            if (!inStock) {
                allInStock = false;
            }

            java.math.BigDecimal unitPrice = combo.getPrice() != null ? combo.getPrice() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal subtotal = unitPrice.multiply(java.math.BigDecimal.valueOf(totalQty));
            totalEstimatedPrice = totalEstimatedPrice.add(subtotal);

            String region = combo.getRegion();
            String regionDisplay = buildPlanMapper.formatRegionDisplayName(region);
            String cookingSuggestion = buildPlanMapper.resolveCookingSuggestion(combo);

            items.add(com.sang.be_capnong.dto.response.ShoppingListItemResponse.builder()
                    .comboId(combo.getComboId())
                    .comboName(combo.getComboName())
                    .imageUrl(combo.getImageUrl())
                    .shopId(combo.getShop() != null ? combo.getShop().getId() : null)
                    .shopName(combo.getShop() != null ? combo.getShop().getShopName() : null)
                    .region(region)
                    .regionDisplayName(regionDisplay)
                    .comboType(combo.getType() != null ? combo.getType() : "Rau củ quả tươi")
                    .cookingSuggestion(cookingSuggestion)
                    .totalQuantity(totalQty)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .stockAvailable(stockAvailable)
                    .inStock(inStock)
                    .build());
        }

        return com.sang.be_capnong.dto.response.ShoppingListResponse.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .totalEstimatedPrice(totalEstimatedPrice)
                .totalCombosCount(items.size())
                .allInStock(allInStock)
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public com.sang.be_capnong.dto.response.ShoppingListResponse addPlanToCart(int planId) {
        com.sang.be_capnong.dto.response.ShoppingListResponse shoppingList = getShoppingList(planId);

        for (com.sang.be_capnong.dto.response.ShoppingListItemResponse item : shoppingList.getItems()) {
            if (item.isInStock()) {
                cartService.addToCart(com.sang.be_capnong.dto.request.CartAddRequest.builder()
                        .itemType("BUILD_COMBO")
                        .itemId(item.getComboId())
                        .quantity(item.getTotalQuantity())
                        .build());
            }
        }

        return shoppingList;
    }

    @Override
    @Transactional
    public PlanMealResponse regenerateMeal(int planId, int mealId) {
        PlanMeal meal = findMealInPlanAndCheckOwnership(planId, mealId);
        BuildPlan plan = meal.getPlanDay().getBuildPlan();

        Integer buyerId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Allergen> buyerAllergies = userAllergyRepository.findByBuyer_Id(buyerId).stream()
                .map(UserAllergy::getAllergen)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<BuildCombo> allCombos = buildComboRepository.findAll();
        List<Integer> allProductIds = allCombos.stream()
                .filter(c -> c.getProductCombos() != null)
                .flatMap(c -> c.getProductCombos().stream())
                .filter(pc -> pc.getProduct() != null)
                .map(pc -> pc.getProduct().getProductId())
                .distinct()
                .toList();

        java.util.Map<Integer, NutritionInfo> nutritionMap = nutritionInfoRepository.findByProduct_ProductIdIn(allProductIds).stream()
                .filter(n -> n.getProduct() != null)
                .collect(java.util.stream.Collectors.toMap(n -> n.getProduct().getProductId(), n -> n, (a, b) -> a));

        List<com.sang.be_capnong.service.mealplan.CandidateComboDto> candidates = candidateSelector.selectCandidates(
                allCombos,
                buyerAllergies,
                buyer.getDietPreference(),
                java.util.Set.of(meal.getMealType()),
                nutritionMap,
                30
        );

        // Exclude combo currently in this meal
        java.util.Set<Integer> currentComboIds = meal.getPlanMealItems().stream()
                .filter(i -> i.getBuildCombo() != null)
                .map(i -> i.getBuildCombo().getComboId())
                .collect(java.util.stream.Collectors.toSet());

        List<com.sang.be_capnong.service.mealplan.CandidateComboDto> eligible = candidates.stream()
                .filter(c -> !currentComboIds.contains(c.getComboId()))
                .toList();

        if (eligible.isEmpty()) {
            eligible = candidates;
        }

        if (eligible.isEmpty()) {
            throw new AppException(ErrorCode.NO_SUITABLE_COMBOS);
        }

        com.sang.be_capnong.service.mealplan.CandidateComboDto chosen = eligible.get(0);
        BuildCombo chosenEntity = buildComboRepository.findById(chosen.getComboId())
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        int people = plan.getNumberOfPeople() != null && plan.getNumberOfPeople() > 0 ? plan.getNumberOfPeople() : 2;
        int qty = Math.max(1, (int) Math.ceil((double) people / 3.0));

        List<PlanMealItem> oldItems = new java.util.ArrayList<>(meal.getPlanMealItems());
        meal.getPlanMealItems().clear();
        planMealItemRepository.deleteAll(oldItems);

        PlanMealItem newItem = PlanMealItem.builder()
                .planMeal(meal)
                .buildCombo(chosenEntity)
                .quantity(qty)
                .unitPriceSnapshot(chosenEntity.getPrice())
                .isAiSuggested(true)
                .userOverride(false)
                .position(1)
                .build();
        meal.getPlanMealItems().add(newItem);
        planMealItemRepository.save(newItem);

        plan.setUpdatedAt(LocalDateTime.now());
        return buildPlanMapper.toPlanMealResponse(meal);
    }
}
