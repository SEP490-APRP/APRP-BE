package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "plan_meal_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanMealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    Integer quantity;
    BigDecimal unitPriceSnapshot;
    Boolean isAiSuggested;
    Integer position;
    Boolean userOverride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_meal_id", nullable = false)
    @JsonIgnore
    PlanMeal planMeal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    BuildCombo buildCombo;
}
