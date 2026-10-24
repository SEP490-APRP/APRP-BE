package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.enums.PlanSource;
import com.sang.be_capnong.enums.PlanStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildPlanResponse {
    int planId;
    Integer buyerId;
    String planName;
    Integer numberOfDays;
    Integer numberOfPeople;
    String mealType;
    BigDecimal targetBudget;
    BigDecimal totalCost;
    PlanStatus status;
    PlanSource source;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<PlanDayResponse> planDays;
    List<String> warnings;
}
